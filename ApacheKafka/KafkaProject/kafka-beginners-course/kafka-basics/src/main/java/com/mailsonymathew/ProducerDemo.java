package com.mailsonymathew;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {

    // Define Logger for this class
    private static final Logger producerLog = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());


    public static void main(String[] args) {

        // Initial Logging message
        producerLog.info("I am a Kafka Producer");

        // Create Producer Properties
        Properties producerProperties = new Properties();  // create producer properties object
        // Now place the configuration properties for teh producer into the properties object whic can be set usign a key value pair as follows
        producerProperties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");  // provide the bootstrap server url. In our demo we are using local host viz, 127.0.0.1 and port 9092
        producerProperties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());  // Serialize the key in the producer message using a string serializer
        producerProperties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()); // Serialize the value in the producer message using a string serializer


        // Create the Producer
        // Note: Here within the <> we are specifying that the producer messages key and value both will be Strings
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);


        // Create a producer record
        // While creating the record, we need to specify the topic-name and the key/value or optionally only the topic and value. Refer ProducerRecord constructor documentation for more options.
        // Note: As a pre-requisite , we have to create the topic "demo_java" in the kafka broker for this to work!!!
        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>("demo_java", "Name", "Tiger");
                //new ProducerRecord<>("demo_java", "Hello Tigers!!!");


        // Send the data - asynchronous operation : This means that the next code lien will be executed without waiting for record to be sent to the KAfka topic
        producer.send(producerRecord);

        // Flush data - synchronous
        // Makes sure that the data upto this line of code is sent and received by Kafka!!!
        // Optionally you can just use producer.close() below which does both flush and close.
        producer.flush();

        // Flush and close producer
        producer.close();



    }
}
