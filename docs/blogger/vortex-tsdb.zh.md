# Vortex TSDB：轻量级分布式时序数据库，HTTP 写入、分钟聚合、窗口查询、内存复制

> **Lightweight. Replicated. Real-time.**
> 一条命令起集群的时序数据库：每个节点都是完整副本，读在本机，写自动复制。

**Vortex TSDB** 是一个面向实时指标的轻量级分布式时序数据库。用 HTTP 写入数值样本，读回按分钟聚合的
count / max / min / sum / avg、每条序列的最新值，以及任意范围内的滚动或滑动窗口。集群自行选主、
复制和快照，不依赖 ZooKeeper、消息队列或外部数据库。

| 版本 | 镜像（amd64 / arm64） | 源码 | License |
|---|---|---|---|
| **1.0.0** | [`fredfeng033/vortex-tsdb`](https://hub.docker.com/r/fredfeng033/vortex-tsdb) | [GitHub](https://github.com/paganini2008/vortex) | Apache 2.0 |

![Query explorer：最近 1 小时、每分钟一个点的 5 分钟滑动窗口](https://paganini2008.github.io/vortex/blogger/assets/query-explorer.png)

---

## 1. 它解决什么问题？

设备在线数、接口 QPS、订单量、温度这类实时指标，需要的是：多个客户端并发写入时计数精确、
能按分钟和任意窗口聚合、节点挂了不丢服务。通用 TSDB 能做到，但往往意味着磁盘存储引擎、
查询语言和多组件运维，对“最近一天的实时指标”来说太重。

Vortex 只做这一层：**数值样本写入、分钟级聚合、窗口查询、多节点高可用**，全部在内存中完成，
Redis 只是可选的溢出存储。

| 痛点 | Vortex 的做法 |
|---|---|
| 部署组件多、运维重 | 一个 Spring Boot 服务 + 可选 Redis，`./run-docker.sh` 一键起集群 |
| 并发写入计数不准 | 所有写在 leader 上串行执行再复制，无锁且精确 |
| 窗口聚合要预先建表或写查询语言 | 只有 HTTP 参数：`range`、`step`、`window`、`z`，读时由分钟桶折叠 |
| 单点故障 | 每个节点全量副本，leader 挂掉 5 秒内自动选主 |
| 内存无限增长 | 每节点 key 数有上限，超出部分转存 Redis |

---

## 2. Quick Start

> **镜像已上架 Docker Hub，免费拉取即用。** 提供 `linux/amd64` 与 `linux/arm64` 版本，无需安装 JDK、Node，也无需构建；`latest` 始终是最新版本。
>
> ```bash
> docker pull fredfeng033/vortex-tsdb:latest
> docker pull fredfeng033/vortex-tsdb-web:latest
> ```

**方式一：直接用 Docker Hub 镜像（无需构建）**

```bash
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d   # 3 个节点 + Web 控制台 + 网关，默认不起 Redis
```

节点启动约需 20-30 秒。节点通过健康检查之前，网关会把请求都交给 Web 控制台：`/tsd/...` 仍可经它访问，Swagger UI 则返回 404。`docker compose ps` 里每个节点显示 `(healthy)` 即已就绪。

需要 Redis 溢出存储时，设置 `VORTEX_REDIS_HOST`（以及 `VORTEX_REDIS_PASSWORD` 等）指向你自己的 Redis。

**方式二：从源码构建运行**

```bash
git clone https://github.com/paganini2008/vortex.git
cd vortex
./run-docker.sh            # 3 个节点 + Redis + Traefik 网关（附 Web 控制台）
```

首次运行会构建镜像（几分钟）。之后：

| 入口 | 地址 |
|---|---|
| HTTP API | http://localhost:9080/tsd/... |
| Swagger UI | http://localhost:9080/swagger-ui.html |
| Web 控制台 | http://localhost:9080/ |
| Traefik 面板 | http://localhost:9081/dashboard/ |

全部接口都在 Swagger UI 里：

![Swagger UI](https://paganini2008.github.io/vortex/blogger/assets/swagger-ui.png)

推一个数，马上能读回来：

```bash
curl -X POST 'http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=44'
curl 'http://localhost:9080/tsd/last?t=long&c=car&d=speed'
# {"code":1,"msg":"ok","data":{"value":44,"timestamp":1791090941234}, ...}
```

常用命令：

| 命令 | 作用 |
|---|---|
| `./run-docker.sh -n 5` | 5 个节点 |
| `./run-docker.sh --no-web` | 不启动 Web UI |
| `./run-docker.sh --no-redis` | 不启动 Redis，超限 key 直接丢弃 |
| `./run-docker.sh --max-keys 50000` | 每节点内存 key 上限 |
| `./run-docker.sh status` | 容器、集群成员、各节点直连端口 |
| `./run-docker.sh down [--purge]` | 停止（`--purge` 连数据一起删） |

---

## 3. Requirements

| 用途 | 依赖 | 版本 |
|---|---|---|
| Docker 部署（推荐） | Docker + BuildKit | Docker Desktop，或 Docker Engine 23+ |
| 本地跑后端 | JDK / Maven | JDK 17+（镜像用 21）/ Maven 3.9 |
| 本地跑前端 | Node.js | 20+ |
| 溢出存储（可选） | Redis | 7（`run-docker.sh` 自带） |

技术栈：Spring Boot 4.1、[openspreader](https://github.com/chaconne-ai/openspreader)、
Next.js 16、React 19、TypeScript、Tailwind 4、Recharts 3、Traefik v3。

---

## 4. How It Works

```
                      ┌────────────── Docker network ──────────────┐
  browser / client    │                                            │
  ─────────────────▶ Traefik ── /tsd/**, /swagger-ui ──▶ node 1 ◀─┐ │
     :9080            │   │                             node 2 ◀─┤ gossip, leader,
                      │   │                             node 3 ◀─┘ replicated cache
                      │   │                               │ overflow (leader only)
                      │   │                             Redis      │
                      │   └── / ─────────────────────▶ web UI      │
                      └────────────────────────────────────────────┘
```

一次写入的路径：

```
POST /tsd/push ──▶ 任意节点 ──转发──▶ leader 串行执行 4 次缓存写
                                        │  max / min / sum(+count) / 最新值
                                        ▼
                               按操作复制到所有节点 ──▶ 任意节点本地读
```

| 数据 | Key | 结构 |
|---|---|---|
| 分钟桶 | `tsd:<type>:<category>:<dimension>:<bucketStart>` | STATS 聚合（max/min/sum/count），保留期即 TTL |
| 最新值 | `tsd:last:<category>` | Hash，一次读出整个 category |
| 序列目录 | `tsd:catalog` | ZSet，score = 最后采样时间 |
| 集群健康 | `tsd:health` | Hash，每节点每 5 秒写一次 |

要点：

- **写入不加锁也不丢计数**：所有写在 leader 上串行执行，再复制给每个节点。
- **窗口不存储，读时折叠**：1 小时、15 分钟滑动窗口，都由分钟桶在本机内存里合并得出。
- **重启不丢数据**：停机时和每 5 分钟写快照；只有 leader 加载快照，其他节点从 leader 全量同步。
- **内存有上限**：超过 `VORTEX_CACHE_MAX_KEYS` 的冷 key 由 leader 写入 Redis，读不到时回 Redis 取。

---

## 5. Code Examples

### Example 1：推送并读取最近一小时（兼容旧版 Vortex 的接口）

**Input**

```bash
for v in 44 67 112; do
  curl -s -X POST "http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=$v"
done
curl -s 'http://localhost:9080/tsd/retrieve?t=long&c=car&d=speed&z=Asia/Shanghai'
```

**Execution**：3 个样本落进同一个分钟桶；`retrieve` 返回最近 60 个桶，按 `z` 时区标注。

**Output**（节选）

```json
{
  "code": 1, "msg": "ok",
  "data": {
    "dataType": "long", "category": "car", "dimension": "speed",
    "data": {
      "09:28:00": {"count": 0, "highestValue": null, "lowestValue": null, "totalValue": null, "averageValue": null},
      "09:29:00": {"count": 3, "highestValue": 112, "lowestValue": 44, "totalValue": 223, "averageValue": 74.3333}
    }
  }
}
```

### Example 2：6 小时范围，15 分钟滑动窗口，每 5 分钟一个点

**Input**

```bash
curl -s 'http://localhost:9080/tsd/query?t=long&c=car&d=speed&range=6h&step=5&window=15&z=Asia/Shanghai'
```

**Execution**：72 个点，每个点合并它之前 15 个分钟桶；`step` = `window` 时就是滚动窗口。

**Output**（节选）

```json
{
  "dataType": "long", "category": "car", "dimension": "speed",
  "range": "6h", "step": 5, "window": 15,
  "last":    {"value": 61, "timestamp": 1791090941234},
  "current": {"count": 12, "highestValue": 118, "lowestValue": 40, "averageValue": 73.5},
  "summary": {"count": 21540, "highestValue": 140, "lowestValue": 1, "averageValue": 70.02},
  "points": [
    {"from": 1791069300000, "to": 1791070200000, "count": 900, "highestValue": 131, "averageValue": 69.8}
  ]
}
```

### Example 3：整个 category 和集群健康

**Input**

```bash
curl -s 'http://localhost:9080/tsd/categories'          # 所有 category，按繁忙程度降序
curl -s 'http://localhost:9080/tsd/category?c=car&range=1h'
curl -s 'http://localhost:9080/tsd/health'              # 每个节点的 QPS、缓存、复制、JVM
```

**Output**（节选）

```json
[{"category": "car", "series": 4, "lastSeen": 1791090941234, "samplesPerMinute": 3600.0}]
```

`/tsd/health` 的数据在 Web 控制台里的样子：

![System health：每个节点的 QPS、缓存、复制和 JVM](https://paganini2008.github.io/vortex/blogger/assets/system-health.png)

### 附带的 Web 控制台

API 之外，镜像里附带一个 Web 控制台，方便开发和运维：

| 页面 | 用途 |
|---|---|
| Query explorer | 任意序列、范围、步长、窗口：图表 + 表格 + 对应的 API 调用 |
| System health | 每个节点的 QPS、错误率、缓存与溢出、复制延迟、leader / 脑裂、JVM |
| Categories / Display | 浏览所有序列，按 category 查看最新值和趋势 |

---

## 6. Configuration

每个组件一份 `.env`（从 `.env.example` 复制）；环境变量优先于文件。

| 文件 | 谁读 | 管什么 |
|---|---|---|
| `.env`（仓库根目录） | `run-docker.sh` | 节点数、是否起 Web / Redis、端口 |
| `backend/tsdb-service/.env` | 每个节点 | 存储、集群、Redis |
| `frontend/tsdb-web/.env` | Docker 外运行的 Web UI | API 地址、开发端口 |

常用节点配置：

| Variable | Default | Description |
|---|---|---|
| `VORTEX_SPAN_MINUTES` | `1` | 桶宽（分钟），需整除 60 |
| `VORTEX_RETENTION` | `24h` | 数据保留期 |
| `VORTEX_TIME_ZONE` | `UTC` | 请求未指定 `z` 时的时区 |
| `VORTEX_SNAPSHOT_INTERVAL` | `5m` | 快照间隔，`0` 为仅停机时写 |
| `VORTEX_CACHE_MAX_KEYS` | `10000` | 每节点内存 key 上限 |
| `VORTEX_REDIS_HOST` | 空 | 溢出 Redis；为空则超限 key 被删除 |
| `VORTEX_PORT_RANGE` | `50000-60000` | 未指定端口时的随机端口范围 |

---

## 7. Performance

**测试环境**：Docker Desktop（4 CPU / 8 GB），3 个节点，k6 经 Traefik 网关压测。

| 场景 | 结果 |
|---|---|
| 阶梯加压 | 826,781 次写入 **0 失败**，三个节点计数完全一致 |
| 吞吐拐点 | 约 **1,000 samples/s**（leader 约 6,800 次缓存操作/s） |
| 峰值 | **1,400–1,500 samples/s**，leader CPU 约 230%，堆 ≤ 310 MB |
| `kill -9` leader（600/s 写入中） | **4.7 s** 选出新 leader；314,091 次请求中 869 次失败（0.28%），集中在被杀的那一刻 |
| 优雅停机 leader | 立即交接，无失败请求 |
| 全部 `kill -9` 后重启 | 恢复到最近一次快照 |
| 12,000 条序列，上限 10,000 key | 3,393 个 key 溢出到 Redis，读取结果正确 |

与常见方案的定位对比：

| | Vortex TSDB | Prometheus | InfluxDB | TimescaleDB |
|---|---|---|---|---|
| 写入方式 | HTTP push | 定期 pull | HTTP push | SQL |
| 查询方式 | HTTP 参数 | PromQL | InfluxQL / SQL | SQL |
| 存储 | 内存全量副本 + 快照 | 本地磁盘 | 磁盘 | PostgreSQL |
| 外部依赖 | 无（Redis 可选） | 无 | 无 | PostgreSQL |
| 高可用 | 内置，自动选主 | 需额外方案 | 视版本而定 | 依赖 PostgreSQL 方案 |
| 适合 | 实时指标、天级保留 | 监控告警 | 通用时序 | 长期分析 |

---

## 8. Limitations & Trade-offs

- **写入不随节点数扩展**：所有写在 leader 串行执行，上限约 1,500 samples/s。
- **数据在内存**：每个节点一份全量副本；全部节点同时被杀会丢失最近一个快照之后的数据（默认最多 5 分钟）。
- **复制是异步的**：leader 已确认但未广播的写入在其崩溃时丢失；leader 崩溃那一刻进行中的写会失败，客户端需重试。
- **Redis 更慢**：命中溢出 key 需要一次网络往返，应让热数据留在内存里。
- **精度**：聚合值为 double，`long` 在 2^53 内精确，`decimal` 保留 8 位小数。
- **不提供**：查询语言、告警、鉴权。

**不适合**：金融账务等不能丢一条的数据、需要月 / 年级保留的分析、每秒数万次以上的写入。

---

## 9. Summary

1. **一条命令起集群**：`./run-docker.sh` 拉起节点、网关和 Redis。
2. **只有 HTTP**：`push` 写入，`query` 带 `range` / `step` / `window` 查询，不用学查询语言。
3. **任意节点可读写**：读走本机内存，写由 leader 串行并复制，计数精确。
4. **滚动与滑动窗口**：由分钟桶在读时折叠，不额外占用存储。
5. **高可用**：leader 被杀 5 秒内恢复，优雅停机无感知。
6. **内存可控**：每节点 key 上限，冷数据溢出到 Redis。
7. **重启不丢**：快照 + leader 加载 + 全量同步。
8. **可观测**：`/tsd/health` 给出每个节点的 QPS、缓存、复制延迟和 JVM 状态。
9. **兼容旧版 Vortex**：`/tsd/push`、`/tsd/test`、`/tsd/retrieve` 参数和返回格式不变。
10. **定位清晰**：轻量级实时 TSDB，天级保留；长期存储与复杂分析交给重量级 TSDB。

项目地址：https://github.com/paganini2008/vortex · License：Apache 2.0
