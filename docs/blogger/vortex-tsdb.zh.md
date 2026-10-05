# Vortex TSDB：一条命令起集群的实时指标库 —— HTTP 写入、分钟聚合、滑动窗口、内存复制

## 1. 概览

> **Lightweight. Replicated. Real-time.**

**Vortex TSDB** 是面向实时指标的分布式时序数据库：用 HTTP 写入数值，读回分钟级聚合、瞬时值以及
滚动 / 滑动窗口。每个节点在内存里持有全量数据。

| 版本 | 镜像（amd64 · arm64） | 源码 | License |
|---|---|---|---|
| **1.0.0** | [`fredfeng033/vortex-tsdb`](https://hub.docker.com/r/fredfeng033/vortex-tsdb) · `ghcr.io/paganini2008/vortex-tsdb` | [GitHub](https://github.com/paganini2008/vortex) | Apache 2.0 |

![Vortex TSDB Web 控制台](https://paganini2008.github.io/vortex/blogger/assets/dashboard.png)

## 2. 它解决什么问题？

设备在线数、接口 QPS、订单量、温度这类实时指标，需要：多客户端并发写入时计数精确、按分钟和任意窗口
聚合、节点挂了服务不停。通用 TSDB 能做到，但要带上磁盘引擎、查询语言和多组件运维；Vortex 只做这一层，
全部在内存里，接口只有 HTTP。

| 痛点 | Vortex |
|---|---|
| 组件多、部署运维重 | 一个服务 + 可选 Redis；`docker compose up -d` |
| 并发写入计数不准 | 写入在 leader 上串行执行再复制：精确、无锁 |
| 窗口聚合要建表或写查询语言 | `range` · `step` · `window` · `z`，读时折叠 |
| 单点故障 | 每个节点全量副本；约 5 秒选出新 leader |
| 内存随序列增长 | 每节点 key 上限；超出部分溢出到 Redis |

## 3. 快速开始

> **镜像已上架 Docker Hub 和 GHCR，免费拉取** · `linux/amd64` + `linux/arm64` · 无需 JDK、Node，无需构建

```bash
curl -fsSLO https://raw.githubusercontent.com/paganini2008/vortex/main/deploy/docker-compose.yml
docker compose up -d                     # 3 个节点 + Web 控制台 + 网关

curl -X POST 'http://localhost:9080/tsd/push?t=long&c=car&d=speed&v=44'
curl 'http://localhost:9080/tsd/last?t=long&c=car&d=speed'
```

```json
{"code":1,"data":{"value":44,"timestamp":1791182928819},"elapsed":1,"msg":"ok","requestPath":"/tsd/last"}
```

| 打开 | 地址 |
|---|---|
| Web 控制台 | http://localhost:9080/ |
| Swagger UI | http://localhost:9080/swagger-ui.html |

| 其他方式 | 命令 |
|---|---|
| 只拉镜像 | `docker pull fredfeng033/vortex-tsdb:latest` · `docker pull fredfeng033/vortex-tsdb-web:latest` |
| 从 GHCR | `docker pull ghcr.io/paganini2008/vortex-tsdb:latest` · `VORTEX_REGISTRY=ghcr.io/paganini2008 docker compose up -d` |
| 接自己的 Redis | `VORTEX_REDIS_HOST=host.docker.internal VORTEX_REDIS_PASSWORD=secret docker compose up -d` |
| 从源码 | `git clone https://github.com/paganini2008/vortex.git && cd vortex && ./run-docker.sh` |

> 节点启动约需 20–30 秒，`docker compose ps` 显示 `(healthy)` 即就绪。

## 4. 环境要求

| 用途 | 需要 |
|---|---|
| 运行镜像 | Docker + compose v2（Docker Desktop、Rancher Desktop、OrbStack、Engine 23+） |
| 溢出存储（可选） | Redis，已测试 7.4 和 8.6 |
| 从源码构建 | JDK 17+、Maven 3.9、Node.js 20+ |
| 技术栈 | Spring Boot 4.1 · openspreader · Next.js 16 · React 19 · Tailwind 4 · Traefik v3 |

## 5. 工作原理

![架构](https://paganini2008.github.io/vortex/blogger/assets/architecture.png)

![一次写入、一次读取](https://paganini2008.github.io/vortex/blogger/assets/dataflow.png)

![滚动与滑动窗口](https://paganini2008.github.io/vortex/blogger/assets/windows.png)

| 数据 | Key | 结构 |
|---|---|---|
| 分钟桶 | `tsd:<type>:<category>:<dimension>:<minute>` | max · min · sum · count；保留期即 TTL |
| 最新值 | `tsd:last:<category>` | Hash：一次读出整个 category |
| 序列目录 | `tsd:catalog` | 按最后采样时间排序的 ZSet |
| 集群健康 | `tsd:health` | Hash；每节点每 5 秒写一次 |

## 6. 代码示例

### 示例 1 —— 写入并读回

| | |
|---|---|
| **输入** | `for v in 44 67 112; do curl -X POST "localhost:9080/tsd/push?t=long&c=car&d=speed&v=$v"; done`，再 `curl 'localhost:9080/tsd/retrieve?t=long&c=car&d=speed&z=Asia/Shanghai'` |
| **执行** | 3 个样本落进同一个分钟桶；`retrieve` 按时区 `z` 返回最近 60 个桶 |
| **输出** | `"09:29:00": {"count": 3, "highestValue": 112, "lowestValue": 44, "totalValue": 223, "averageValue": 74.3333}` |

### 示例 2 —— 滑动窗口

| | |
|---|---|
| **输入** | `curl 'localhost:9080/tsd/query?t=long&c=car&d=speed&range=6h&step=5&window=15'` |
| **执行** | 72 个点，每个点折叠它之前的 15 个分钟桶；`window = step` 即滚动窗口 |
| **输出** | `{"step": 5, "window": 15, "last": {"value": 61}, "summary": {"count": 21540, …}, "points": [{"from": …, "to": …, "count": 900, "averageValue": 69.8}, …]}` |

![Query explorer](https://paganini2008.github.io/vortex/blogger/assets/query-explorer.png)

### 示例 3 —— 整个集群

| | |
|---|---|
| **输入** | `curl 'localhost:9080/tsd/categories'` · `curl 'localhost:9080/tsd/health'` |
| **执行** | 任意节点都从本机的复制内存中作答 |
| **输出** | 按繁忙程度排序的 category；每个节点的 QPS、错误率、缓存、复制延迟、JVM |

![System health](https://paganini2008.github.io/vortex/blogger/assets/system-health.png)

| Categories | 放大 | 浅色主题 |
|---|---|---|
| ![](https://paganini2008.github.io/vortex/blogger/assets/categories.png) | ![](https://paganini2008.github.io/vortex/blogger/assets/zoom.png) | ![](https://paganini2008.github.io/vortex/blogger/assets/dashboard-light.png) |

## 7. 配置

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VORTEX_RETENTION` | `24h` | 数据保留期 |
| `VORTEX_SPAN_MINUTES` | `1` | 桶宽（分钟），需整除 60 |
| `VORTEX_TIME_ZONE` | `UTC` | 请求未带 `z` 时的时区 |
| `VORTEX_CACHE_MAX_KEYS` | `10000` | 每节点内存 key 上限 |
| `VORTEX_REDIS_HOST` | 空 | 溢出 Redis；为空则超限 key 被删除 |
| `VORTEX_SNAPSHOT_INTERVAL` | `5m` | 快照间隔 |
| `VORTEX_GATEWAY_PORT` | `9080` | 宿主机端口 |

全部配置见 [docs/configuration.md](https://github.com/paganini2008/vortex/blob/main/docs/configuration.md)。

## 8. 性能与对比

**环境：** Docker Desktop，4 CPU / 8 GB，3 个节点，k6 经网关压测。

| 场景 | 结果 |
|---|---|
| 阶梯加压 826,781 次写入 | **0 失败**，各节点计数完全一致 |
| 持续吞吐 | **约 1,000 samples/s** |
| 峰值 | **1,400–1,500 samples/s**，leader 约 2.3 个 CPU，堆 ≤ 310 MB |
| 600/s 写入时 `kill -9` leader | **4.7 秒**选出新 leader，0.28% 请求失败 |
| 优雅停机 leader | 立即交接，0 失败 |

|  | Vortex | Prometheus | InfluxDB | TimescaleDB |
|---|---|---|---|---|
| 写入 | HTTP push | pull | HTTP push | SQL |
| 查询 | HTTP 参数 | PromQL | InfluxQL / SQL | SQL |
| 存储 | 内存 + 快照 | 磁盘 | 磁盘 | PostgreSQL |
| 高可用 | 内置 | 需额外方案 | 视版本 | 依赖 PG 方案 |
| 适合 | 实时指标、天级保留 | 监控告警 | 通用时序 | 长期分析 |

## 9. 限制与取舍

| 限制 | 后果 |
|---|---|
| 所有写入经一个 leader | 约 1,000–1,500 samples/s，不随节点数增长 |
| 数据在内存 | 所有节点同时被杀 → 回到最近快照（≤ 5 分钟） |
| 异步复制 | leader 崩溃时进行中的写入会失败，其中少数可能已写入 |
| Redis 溢出 | 读取已溢出的 key 需要一次网络往返 |
| 无查询语言、告警、鉴权 | 部署在内网 |

**不适合：** 一条都不能丢的账务数据 · 月 / 年级保留 · 每秒数万次以上写入。

## 10. 总结

1. **一条命令** —— `docker compose up -d`，镜像支持 amd64 与 arm64。
2. **只有 HTTP** —— 写入用 `t/c/d/v`，查询用 `range/step/window/z`。
3. **计数精确** —— leader 串行执行写入，无漂移、无锁。
4. **本机读取** —— 每个节点都是全量副本。
5. **窗口免费** —— 滚动与滑动窗口都由分钟桶折叠而来。
6. **自动恢复** —— 约 5 秒选出新 leader，优雅停机无感知。
7. **足够持久** —— 停机时及每 5 分钟写快照。
8. **内存可控** —— 每节点 key 上限，可选 Redis 溢出。
9. **可观测** —— `/tsd/health` 与 Web 控制台开箱即用。
10. **定位清晰** —— 面向天级保留的实时指标。

**GitHub：** https://github.com/paganini2008/vortex · **官网：** https://paganini2008.github.io/vortex/
