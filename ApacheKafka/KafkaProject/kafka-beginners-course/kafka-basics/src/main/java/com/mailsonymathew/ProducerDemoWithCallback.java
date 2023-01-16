package com.mailsonymathew;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithCallback {

    // Define Logger for this class
    private static final Logger producerLogWithCallback = LoggerFactory.getLogger(ProducerDemoWithCallback.class.getSimpleName());


    public static void main(String[] args) {

        // Initial Logging message
        producerLogWithCallback .info("I am a Kafka Producer");

        // Create Producer Properties
        Properties producerProperties = new Properties();  // create producer properties object
        // Now place the configuration properties for teh producer into the properties object whic can be set using a key value pair as follows
        producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");  // provide the bootstrap server IP. In our demo we are using local host viz, 127.0.0.1 and port 9092
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());  // Serialize the key in the producer message using a string serializer
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // Serialize the value in the producer message using a string serializer


        // Create the Producer
        // Note: Here within the <> we are specifying that the producer messages key and value both will be Strings
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);


        // Now Produce send a series of messages .... 10 of them actually using a for loop.
        for(int i=0; i<10; i++){

            // create a producer record
            // While creating the record, we need to specify the topic-name and the key/value or optionally only the topic and value. Refer ProducerRecord constructor documentation for more options.
            // Note: As a pre-requisite , we have to create the topic "demo_java" in the kafka broker for this to work!!!
            ProducerRecord<String, String> producerRecord =
                    // Sticky Partitioner:
                    //Normally when we produce without a key, we will expect the messages to go randomly to partitions 0,1,2,
                    // But in this case you can observe all msgs go to the same partition.
                    // This is because when we send a series of msgs quickly as in this case, the producer batches the msgs to improve effciency
                    // Messages from the same batch will be sent to the same partition!!!
                    new ProducerRecord<>("demo_java", "Jungle " + i);
                    //since we are using a new key for each write the values will be randomly distributed across the partitions 0,1,2
                    //new ProducerRecord<>("demo_java", "Name" + i, "Hello Tiger" + i);
                    //since we are using teh same key for each write the values will be written to the same partition!!!
                    //new ProducerRecord<>("demo_java", "Name" , "Roar" + i);

            // send the data - asynchronous using callbacks
            producer.send(producerRecord, new Callback() {

                // Since we are using Callback, we need to implement the onCompletion method
                // onCompletion will be called whenever the message is successfully completed and send to Kafka
                @Override
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    // executes every time a record is successfully sent or an exception is thrown
                    if (e == null){ // no exception!!!
                        // the record was successfully sent :)
                        // log some of that metadata...
                        producerLogWithCallback.info("Received new metadata :) \n" +
                                "Topic: " + metadata.topic() + "\n" +
                                "Partition: " + metadata.partition() + "\n" +
                                "Offset: " + metadata.offset() + "\n" +
                                "Timestamp: " + metadata.timestamp());
                    } else { // log the exception!!!
                        producerLogWithCallback.error("Bugger...error while producing :( ", e);
                    }
                }
            });

//      Note: Attempt to force producer to start a new batch by puttings the current thread to sleep.
//            try {
//                Thread.sleep(10000);  // 10 secs
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        // flush data - synchronous
        // Flush data - synchronous
       // Makes sure that the data upto this line of code is sent and received by Kafka!!!
       // Optionally you can just use producer.close() below which does both flush and close.

        producer.flush();

        // flush and close producer
        producer.close();

    }

}
