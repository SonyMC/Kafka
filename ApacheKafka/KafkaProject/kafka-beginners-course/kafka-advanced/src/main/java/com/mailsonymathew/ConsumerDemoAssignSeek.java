package com.mailsonymathew;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoAssignSeek {
    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(ConsumerDemoAssignSeek.class.getName());

        /*
        Some variables we will be needing soon
         */
        String topic = "demo_java";  // topic has already been created in kafka
        String boostrapServers = "127.0.0.1:9092";  // Server address
        /*
        Remove the group.id from the consumer properties (we don't use consumer groups anymore)
         */
        //String groupId = "my-second-consumer-group";  // consumer group has already been created in kafka

       /*
        Create consumer configs
        */
        Properties properties = new Properties();  // create producer properties object
        /*
         Now place the configuration properties for the consumer into the properties object which can be set using a key value pair as follows
         */
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers); // server property
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the key in the consumer message using a string serializer
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the value in the consumer message using a string serializer
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // values = none/earliest/latest.  Here we have chosen to read from the earliest entry
        /*
         Remove the group.id from the consumer properties (we don't use consumer groups anymore)
         */
        //properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId); // group-id



        /*
         Create consumer
         */
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

        /*
         Assign and seek are mostly used to replay data or fetch a specific message
         */

        // assign
        TopicPartition partitionToReadFrom = new TopicPartition(topic, 0);  // Read from Partition 0
        long offsetToReadFrom = 7L;  // Offset to be read from
        consumer.assign(Arrays.asList(partitionToReadFrom));

        // seek  data from teh assigned parttion
        consumer.seek(partitionToReadFrom, offsetToReadFrom);

        int numberOfMessagesToRead = 5;
        boolean keepOnReading = true;
        int numberOfMessagesReadSoFar = 0;

        // poll for new data
        while(keepOnReading){
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> record : records){
                numberOfMessagesReadSoFar += 1;
                log.info("Key: " + record.key() + ", Value: " + record.value());
                log.info("Partition: " + record.partition() + ", Offset:" + record.offset());
                if (numberOfMessagesReadSoFar >= numberOfMessagesToRead){
                    keepOnReading = false; // to exit the while loop
                    break; // to exit the for loop
                }
            }
        }

        log.info("Exiting the application");
    }
}
