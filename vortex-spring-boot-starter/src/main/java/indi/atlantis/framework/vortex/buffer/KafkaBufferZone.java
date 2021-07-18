/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package indi.atlantis.framework.vortex.buffer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.github.paganini2008.devtools.StringUtils;

import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.utils.FstKafkaSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * KafkaBufferZone
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
@Slf4j
public class KafkaBufferZone implements BufferZone {

	private final AtomicLong counter = new AtomicLong();
	private String bootstrapServers;
	private String topicName;
	private String groupId;
	private int pullSize;

	private KafkaProducer<String, Tuple> kafkaProducer;
	private KafkaConsumer<String, Tuple> kafkaConsumer;

	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public void setPullSize(int pullSize) {
		this.pullSize = pullSize;
	}

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
		p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

		kafkaConsumer = new KafkaConsumer<String, Tuple>(p);
		kafkaConsumer.subscribe(Collections.singleton(topicName));

		log.info("KafkaBufferZone configure successfully.");
	}

	@Override
	public void set(String topic, Tuple tuple) throws Exception {
		if (StringUtils.notEquals(topic, this.topicName)) {
			throw new IllegalArgumentException("Mismatched topic: " + topic);
		}
		ProducerRecord<String, Tuple> record = new ProducerRecord<String, Tuple>(topic, tuple);
		kafkaProducer.send(record);
		counter.incrementAndGet();
	}

	@Override
	public List<Tuple> get(String topic, int pullSize) throws Exception {
		if (StringUtils.notEquals(topic, this.topicName)) {
			throw new IllegalArgumentException("Mismatched topic: " + topic);
		}
		List<Tuple> list = new ArrayList<Tuple>();
		ConsumerRecords<String, Tuple> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
		for (ConsumerRecord<String, Tuple> record : consumerRecords) {
			list.add(record.value());
			counter.decrementAndGet();
		}
		return list;
	}

	@Override
	public long size(String topic) throws Exception {
		if (StringUtils.notEquals(topic, this.topicName)) {
			throw new IllegalArgumentException("Mismatched topic: " + topic);
		}
		return counter.get();
	}

	public void close() {
		if (kafkaProducer != null) {
			kafkaProducer.close();
		}
		if (kafkaConsumer != null) {
			kafkaConsumer.close();
		}
	}

}
