package com.mailsonymathew;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {

    // Define Logger for this class
    private static final Logger producerLogWithCallback = LoggerFactory.getLogger(ProducerDemoKeys.class.getSimpleName());


    public static void main(String[] args) {

        // Initial Logging message
        producerLogWithCallback .info("I am a Kafka Producer");

        // Create Producer Properties
        Properties producerProperties = new Properties();  // create producer properties object
        // Now place the configuration properties for the producer into the properties object which can be set using a key value pair as follows
        producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");  // provide the bootstrap server url. In our demo we are using local host viz, 127.0.0.1 and port 9092
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());  // Serialize the key in the producer message using a string serializer
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // Serialize the value in the producer message using a string serializer


        // Create the Producer
        // Note: Here within the <> we are specifying that the producer messages key and value both will be Strings
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);


        // Now Produce send a series of messages .... 10 of them actually using a for loop.
        for(int i=0; i<10; i++){

            String topic = "demo_java";
            String value1 = "Make some noise !!!" + i;
            String value2 = "Keep Roaring !!!" + i;
            String key1 = "Tiger" + i;
            String key2 = "Tiger";

            // create a producer record
            // While creating the record, we need to specify the topic-name and the key/value or optionally only the topic and value. Refer ProducerRecord constructor documentation for more options.
            // Note: As a pre-requisite , we have to create the topic "demo_java" in the kafka broker for this to work!!!
            ProducerRecord<String, String> producerRecord =
                    //Since we are using a new key for each write the values will be randomly distributed across the partitions 0,1,2.
                    // However the records with same key will be written to the same partition .
                    // This can be demonstrated by running teh program twice.
                    new ProducerRecord<>(topic, key1, value1);
                    //Since we are using the same key for each write the values will be written to the same partition!!!
                    //new ProducerRecord<>(topic, key2 , value2);

            // send the data - asynchronous using callbacks
            producer.send(producerRecord, new Callback() {

                // Since we are using Callback, we need to implement the onCompletion method
                // onCompletion will be called whenever the message is successfully completed and send to Kafka
                @Override
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    // executes every time a record is successfully sent or an exception is thrown
                    if (e == null){ // no exception!!!
                        // the record was successfully sent :)
                        // log ...
                        producerLogWithCallback.info("Received new metadata :) \n" +
                                "Topic: " + metadata.topic() + "\n" +
                                "Key: " + producerRecord.key() + "\n" +
                                "Partition: " + metadata.partition() + "\n" +
                                "Offset: " + metadata.offset() + "\n" +
                                "Timestamp: " + metadata.timestamp());
                    } else { // log the exception!!!
                        producerLogWithCallback.error("Bugger...error while producing :( ", e);
                    }
                }
            });


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
