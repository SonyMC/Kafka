package com.mailsonymathew.kafka.streams;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BankTransactionsProducer {
	public static void main(String[] args) {
        Properties properties = new Properties();

        // kafka bootstrap server
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // producer acks
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all"); // strongest producing guarantee - Leader and ISRs(In Syc Replicas) have acknowledged receiving the data
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, "3");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "1");
        // leverage idempotent producer from Kafka 0.11 !
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); // ensure we don't push duplicates

        Producer<String, String> producer = new KafkaProducer<>(properties);
        /*
        Send some data as JSON in a loop
         */
        int i = 0;
        while (true) {
            System.out.println("Producing batch: " + i);
            try {
                producer.send(newRandomTransaction("john"));
                Thread.sleep(100); // 0.1 seconds
                producer.send(newRandomTransaction("stephane"));
                Thread.sleep(100);
                producer.send(newRandomTransaction("alice"));
                Thread.sleep(100);
                i += 1;
            } catch (InterruptedException e) {
                break;
            }
        }
        producer.close();
    }

    /*
    Function to generate a JSON document and return a Producer Record
    JSON document E.g: {"name": "John", "amount": 123, "time": "2017-07-19T05:24:42"}
     */
    public static ProducerRecord<String, String> newRandomTransaction(String name) {
        // creates an empty json {}
        ObjectNode transaction = JsonNodeFactory.instance.objectNode();

        // Generate a random no.  between 0 and 100 excluded)
        // E.g:  { "amount" : 46 } (46 is a random number between 0 and 100 excluded)
        Integer amount = ThreadLocalRandom.current().nextInt(0, 100);

        // Instant.now() is to get the current time using Java 8
        Instant now = Instant.now();

        // we write the data to the json document
        transaction.put("name", name);
        transaction.put("amount", amount);
        transaction.put("time", now.toString());

        // Return the generated Producer Record using the JSON document
        return new ProducerRecord<>("bank-transactions", name, transaction.toString());
    }
}
