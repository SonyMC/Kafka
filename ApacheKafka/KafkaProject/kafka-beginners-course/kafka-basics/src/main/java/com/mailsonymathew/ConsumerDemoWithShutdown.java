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
import java.util.List;
import java.util.Properties;

public class ConsumerDemoWithShutdown {

    // Define the    Logger
    private static final Logger consumerLog = LoggerFactory.getLogger(ConsumerDemoWithShutdown.class.getSimpleName());

    public static void main(String[] args) {

        consumerLog.info("I am a Kafka Consumer");

        // Some variables we will be needing soon
        String topic = "demo_java";  // topic has already been created in kafka
        String groupId = "my-second-consumer-group";  // consumer group has already been created in kafka
        String boostrapServers = "127.0.0.1:9092";  // Server address


        // Set Consumer Properties
        Properties consumerProperties = new Properties();  // create producer properties object
        // Now place the configuration properties for the consumer into the properties object which can be set using a key value pair as follows
        consumerProperties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers);  // server property
        consumerProperties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());  // De-Serialize(bytes into objects) the key in the consumer message using a string serializer
        consumerProperties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the value in the consumer message using a string serializer
        consumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);  // group-id
        consumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // values = none/earliest/latest.  Here we have chosen to read from the earliest entry

        // Create Consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);

        // Get a reference to the current main thread
        final Thread mainThread = Thread.currentThread();


        // adding the shutdown hook as a new thread
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {  // run this thread when we shutdown our application
                consumerLog.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                consumer.wakeup();  // wake up the consumer which will cause an WakeUp expection resulting in theexit of infinite while() loop defined below!!!

                // join the main thread to allow the execution of the code in the main thread
                // Note: Essentiallu the WakeUp exception trigerred above will wait till the main thread has finished execution
                try {
                    mainThread.join();  // join the shutDownHook thread with the main thread
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {

            // Subscribe consumer to our topic(s)
            //consumer.subscribe(Arrays.asList(topic));   // If there are more than one topics, mention it here by separating  it by a comma
            consumer.subscribe(List.of(topic));
            // Poll for data using an infinite loop
            // Note: This infinite while loop will exit when the application is shutdown as consumer.wakeup(0 call will trigger a WakeUp Exception
            while(true) {

                // log entry
                consumerLog.info("Polling");

                // Develop a consumer record

                ConsumerRecords<String, String> records =
                        // -Poll Kakfa and get as many records as you can.
                        // -But if there are no records on Kafka, then wait til 1000 ms .
                        // -If no new recs are found in next 1000ms, got to the next line of code
                        // Note : This is where the 'WakeupException' is trigerred as when it tries to wakeup as the application would be trying to shut down( we would be shutting the application manually)!!!
                        consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    consumerLog.info("Key: " + record.key() + ", Value: " + record.value());
                    consumerLog.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }

            }  // while() bloack ends
        }  // try block ends
        catch (WakeupException e) {
            consumerLog.info("Wake up exception!");
            // we ignore this as this is an expected exception when closing a consumer
        }
        catch (Exception e){
            consumerLog.error("Unexpected exception");
        }
        finally {  //always execute
            consumer.close(); // this will also commit the offsets if need be
            consumerLog.info("The consumer is now gracefully closed");
        }

    }  // main() block ends
}