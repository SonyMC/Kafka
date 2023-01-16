package com.mailsonymathew;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/*
Note : Requires the class 'ComsumerRebalanceListenerImpl' for creating teh listener
 */
public class ConsumerDemoRebalanceListener {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoRebalanceListener.class);

    public static void main(String[] args) {
        log.info("I am a Kafka Consumer with a Rebalance");

        // Some variables we will be needing soon
        String topic = "demo_java";  // topic has already been created in kafka
        String groupId = "my-second-consumer-group";  // consumer group has already been created in kafka
        String boostrapServers = "127.0.0.1:9092";  // Server address

        // create consumer configs
        Properties properties = new Properties();  // create producer properties object
        // Now place the configuration properties for the consumer into the properties object which can be set using a key value pair as follows
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers); // server property
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the key in the consumer message using a string serializer
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the value in the consumer message using a string serializer
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId); // group-id
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // values = none/earliest/latest.  Here we have chosen to read from the earliest entry


        // We disable Auto Commit of offsets (We disable auto commit (otherwise we wouldn't need a rebalance listener)
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // Create listener object using the 'ConsumerRebalanceListenerImpl' class
        ConsumerRebalanceListenerImpl listener = new ConsumerRebalanceListenerImpl(consumer);

        // Get a reference to the current thread
        final Thread mainThread = Thread.currentThread();

        // Adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            // subscribe consumer to our topic(s)
            consumer.subscribe(Arrays.asList(topic), listener);

            // poll for new data
            while (true) {
                ConsumerRecords<String, String> records =
                        consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    log.info("Key: " + record.key() + ", Value: " + record.value());
                    log.info("Partition: " + record.partition() + ", Offset:" + record.offset());

                    //On every message being successfully synchronously processed, we track the offset we have committed in the listener
                    // Allows us to track how far we've been processing in our consumer
                    listener.addOffsetToTrack(record.topic(), record.partition(), record.offset());
                }

                // We commitAsync as we have processed all data and we don't want to block until the next .poll() call
                consumer.commitAsync();
            }
        } catch (WakeupException e) {
            log.info("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        } catch (Exception e) {
            log.error("Unexpected exception", e);
        } finally {
            try {
                /*
                On the consumer shutdown, we finally call again consumer.commitSync(listener.getCurrentOffsets());
                To commit one last time based on how far we've read before closing the consumer.
                 */

                consumer.commitSync(listener.getCurrentOffsets()); // we must commit the offsets synchronously here
            } finally {
                consumer.close();
                log.info("The consumer is now gracefully closed.");
            }
        }
    }

}
