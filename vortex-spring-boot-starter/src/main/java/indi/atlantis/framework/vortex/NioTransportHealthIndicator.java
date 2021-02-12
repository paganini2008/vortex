package indi.atlantis.framework.vortex;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;

/**
 * 
 * NioTransportHealthIndicator
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class NioTransportHealthIndicator extends AbstractHealthIndicator {

	@Qualifier("producer")
	@Autowired
	private Counter producer;

	@Qualifier("consumer")
	@Autowired
	private Counter consumer;

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		long totalTps = producer.totalTps();
		if (totalTps < 2000) {
			builder.status(new Status("Low payload level"));
		} else if (totalTps < 5000) {
			builder.status(new Status("Middle payload level"));
		} else if (totalTps < 10000) {
			builder.status(new Status("Higher payload level"));
		} else {
			builder.status(new Status("High payload level"));
		}
		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("producer:count", producer.count());
		detail.put("producer:totalCount", producer.totalCount());
		detail.put("producer:tps", producer.tps());
		detail.put("producer:totalTps", producer.totalTps());
		detail.put("producer:state", producer.isIdleTimeout(60, TimeUnit.SECONDS) ? "idle" : "active");
		detail.put("consumer:count", consumer.count());
		detail.put("consumer:totalCount", consumer.totalCount());
		detail.put("consumer:tps", consumer.tps());
		detail.put("consumer:totalTps", consumer.totalTps());
		detail.put("consumer:state", consumer.isIdleTimeout(60, TimeUnit.SECONDS) ? "idle" : "active");
		builder.withDetails(detail);
	}

}
