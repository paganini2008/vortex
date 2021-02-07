package indi.atlantis.framework.gearless.buffer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.github.paganini2008.devtools.collection.MapUtils;

import indi.atlantis.framework.gearless.common.Tuple;
import indi.atlantis.framework.gearless.utils.FstKafkaSerializer;
import indi.atlantis.framework.reditools.common.RedisCounter;
import indi.atlantis.framework.seafloor.utils.BeanLifeCycle;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * KafkaBufferZone
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
@Slf4j
public class KafkaBufferZone implements BufferZone, BeanLifeCycle {

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Value("${spring.application.cluster.transport.bufferzone.kafka.bootstrapServers}")
	private String bootstrapServers;

	@Value("${spring.application.cluster.transport.bufferzone.collectionName}")
	private String topicName;

	@Value("${spring.application.cluster.transport.bufferzone.pullSize:100}")
	private int pullSize;

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	private final Map<String, String> keyMapper = new ConcurrentHashMap<String, String>();
	private final Map<String, RedisCounter> counter = new ConcurrentHashMap<String, RedisCounter>();

	private KafkaProducer<String, Tuple> kafkaProducer;
	private KafkaConsumer<String, Tuple> kafkaConsumer;

	@Override
	public void configure() throws Exception {
		Properties p = new Properties();
		p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, FstKafkaSerializer.class);
		kafkaProducer = new KafkaProducer<String, Tuple>(p);

		p = new Properties();
		p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, pullSize);
		p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
		p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, FstKafkaSerializer.class);
		p.put(ConsumerConfig.GROUP_ID_CONFIG, "spring.application.cluster.transport." + clusterName);

		kafkaConsumer = new KafkaConsumer<String, Tuple>(p);
		kafkaConsumer.subscribe(Arrays.asList(topicName));

		log.info("KafkaBufferZone configure successfully.");
	}

	@Override
	public void set(String topic, Tuple tuple) throws Exception {
		ProducerRecord<String, Tuple> record = new ProducerRecord<String, Tuple>(topic, tuple);
		kafkaProducer.send(record);
		MapUtils.get(counter, topic, () -> {
			return new RedisCounter(keyFor(topic), redisConnectionFactory);
		}).incrementAndGet();
	}

	@Override
	public List<Tuple> get(String topic, int pullSize) throws Exception {
		List<Tuple> list = new ArrayList<Tuple>();
		ConsumerRecords<String, Tuple> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
		for (ConsumerRecord<String, Tuple> record : consumerRecords) {
			list.add(record.value());
			MapUtils.get(counter, record.topic(), () -> {
				return new RedisCounter(keyFor(topic), redisConnectionFactory);
			}).decrementAndGet();
		}
		return list;
	}

	@Override
	public long size(String topic) throws Exception {
		String key = keyFor(topic);
		return counter.containsKey(key) ? counter.get(key).get() : 0;
	}

	protected String keyFor(String topic) {
		return MapUtils.get(keyMapper, topic, () -> {
			return String.format(DEFAULT_KEY_FORMAT, clusterName, topic, "");
		});
	}

	@Override
	public void destroy() {
		for (Map.Entry<String, RedisCounter> entry : counter.entrySet()) {
			entry.getValue().destroy();
		}
		log.info("KafkaBufferZone destroy successfully.");
	}

}
