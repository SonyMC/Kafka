package com.mailsonymathew;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class ConsumerDemo {

    // Define teh Logger
    private static final Logger consumerLog = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());

    public static void main(String[] args) {

        consumerLog.info("I am a Kafka Consumer");

        // Some variables we will be needing soon
        String topic = "demo_java";  // topic has already been created in kafka
        String groupId = "my-second-consumer-group";  // consumer group has already been created in kafka
        String boostrapServers = "127.0.0.1:9092";  // Server address


        // Set Consume Properties
        Properties consumerProperties = new Properties();  // create producer properties object
        // Now place the configuration properties for the consumer into the properties object which can be set using a key value pair as follows
        consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);  // server property
        consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());  // De-Serialize(bytes into objects) the key in the consumer message using a string serializer
        consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the value in the consumer message using a string serializer
        consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);  // group-id
        consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // values = none/earliest/latest.  Here we have chosen to read from the earliest entry

        // Create Consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);

        // Consume the messages:
        //   - Subscribe the consumer to the topic
        consumer.subscribe(Collections.singleton(topic));  // subscribe to a single topic
        //consumer.subscribe(Arrays.asList(topic1,topic2,topic3));  // subscribe to multiple topics

        //Poll for data using an infinite loop
        while (true) {
            // log entry
            consumerLog.info("Polling");

            // Develop a consumer record
            ConsumerRecords<String, String> records =
                    // -Poll Kakfa and get as many records as you can.
                    // -But if there are no records on Kafka, then wait til 100 ms .
                    // -If no new recs are found in next 100ms, got to the next line of code
                    consumer.poll(Duration.ofMillis(100));

            // Iterate each Consumer Record
            for (ConsumerRecord<String, String> record : records) {
                // log info about consumed messages
                consumerLog.info("Key: " + record.key() + ", Value: " + record.value());
                consumerLog.info("Partition: " + record.partition() + ", Offset: " + record.offset());
            }


        }
    }
}