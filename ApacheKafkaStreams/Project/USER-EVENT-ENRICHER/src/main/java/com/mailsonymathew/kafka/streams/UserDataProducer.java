package com.mailsonymathew.kafka.streams;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;



// Run the Kafka Streams application before running the producer.
// This will be best for your learning
public class UserDataProducer {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        Properties properties = new Properties();
        /*
        Producer Property Configuration
         */
        // kafka bootstrap server
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // producer acks
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all"); // strongest producing guarantee
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, "3");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "1");
        // leverage idempotent producer from Kafka 0.11 !
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); // ensure we don't push duplicates
        Producer<String, String> producer = new KafkaProducer<>(properties);

        /*
        WARNING
         */
        // FYI - We do .get() to ensure the writes to the topics are sequential, for the sake of the teaching exercise
        // DO NOT DO THIS IN PRODUCTION OR   IN ANY PRODUCER. BLOCKING A FUTURE IS BAD!
        // FUTURE: A FUTURE in Java is something that will complete in future

        // we are going to test different scenarios to illustrate the join

        // 1 - we create a new user, then we send some data to Kafka using producers
        System.out.println("\nExample 1 - new user\n");

        /*
        Note: Since we are doing a .get() here, which means we are forcing a FUTURE to complete . The code will not move on unless that future is completed and hence is blocking which is a bad practice!!!
        We are using a .get() here to ensure that the userRecord arrives before the purchaseRecord.
         */
        producer.send(userRecord("john", "First=John,Last=Doe,Email=john.doe@gmail.com")).get();
        producer.send(purchaseRecord("john", "Apples and Bananas (1)")).get();

        /*
        Sleep for 10 secs
         */
        Thread.sleep(10000);

        // 2 - We receive user purchase, but it doesn't exist in Kafka
        // i.e. We send data only to purchaseRecord and not to UserRecord
        System.out.println("\nExample 2 - non existing user\n");
        producer.send(purchaseRecord("bob", "Kafka Udemy Course (2)")).get();

        /*
        Sleep for 10 secs
         */
        Thread.sleep(10000);

        // 3 - We update user "john", and send a new transaction
        System.out.println("\nExample 3 - update to user\n");
        producer.send(userRecord("john", "First=Johnny,Last=Doe,Email=johnny.doe@gmail.com")).get();  //value has changed
        producer.send(purchaseRecord("john", "Oranges (3)")).get();

         /*
        Sleep for 10 secs
         */
        Thread.sleep(10000);

        // 4 - We send a user purchase for stephane, but it exists in Kafka later
        System.out.println("\nExample 4 - Purchase before User and then Purchase again and Delete uUser\n");
        // Send purchaseRecord before userRecord
        producer.send(purchaseRecord("stephane", "Computer (4)")).get();
        producer.send(userRecord("stephane", "First=Stephane,Last=Maarek,GitHub=simplesteph")).get();
        // Purchase something again and delete the user
        producer.send(purchaseRecord("stephane", "Books (4)")).get();
        // Note: Since we are using a KTable(global) for user , a null value will delete the record for the given key
        producer.send(userRecord("stephane", null)).get(); // delete the user
         /*
        Sleep for 10 secs
         */
        Thread.sleep(10000);

        // 5 - We create a user, but it gets deleted before any purchase comes through
        System.out.println("\nExample 5 - user then delete then data\n");
        // Create User
        producer.send(userRecord("alice", "First=Alice")).get();
        // Delete the user
        producer.send(userRecord("alice", null)).get(); // that's the delete record
        // Create Purchase Record for teh deleted user
        producer.send(purchaseRecord("alice", "Apache Kafka Series (5)")).get();

        Thread.sleep(10000);

        System.out.println("End of demo");
        producer.close();
    }

    /*
    Produce to topic 'user-table' using the provided key.value pair
     */
    private static ProducerRecord<String, String> userRecord(String key, String value){
        return new ProducerRecord<>("user-table", key, value);
    }

    /*
      Produce to topic 'user-purchases' using the provided key.value pair
       */
    private static ProducerRecord<String, String> purchaseRecord(String key, String value){
        return new ProducerRecord<>("user-purchases", key, value);
    }
}
