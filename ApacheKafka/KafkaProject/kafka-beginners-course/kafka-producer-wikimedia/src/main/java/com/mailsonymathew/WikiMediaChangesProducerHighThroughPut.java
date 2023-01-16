package com.mailsonymathew;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikiMediaChangesProducerHighThroughPut {

    public static void main(String[] args) throws InterruptedException {

        String bootstrapServers = "127.0.0.1:9092";
        String topic = "wikimedia.recentchange";  // Is the topic name for picking up the Wikimedia recent changes. We will need to define this topic in our Kafka CLuster
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";  // Wikimedia Recent change stream

        // create Producer Properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // provide the bootstrap server IP. In our demo we are using local host viz, 127.0.0.1 and port 9092
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());  // Serialize the key in the producer message using a string serializer
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());  // Serialize the value in the producer message using a string serializer


        // Set high throughput producer configs
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");  //how long to wait until we send a batch, Default is 0 ms. Here we have set it to 20ms.
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); //max. no. of bytes that will be included in a batch. Her we are setting ii to 32kb. Default size is 16kb
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");  //set producer compression type to 'snappy'. DEfault is 'none'.


        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        // EventHandler allows you to handle the events emanating from the stream  and send them to the producer topics
        EventHandler eventHandler = new WikimediaChangeHandler(producer, topic);   // WikimediaChangeHandler is defined as a separate class which implements the 'EventHandler' interface

        // Create the Event Source using the  url of the Wikimedia Recent change stream
        EventSource.Builder builder = new EventSource.Builder(eventHandler, URI.create(url));
        EventSource eventSource = builder.build();  // Build teh event source


        // Now start the producer via the eventSource in another thread
        eventSource.start();  // The event source contains the eventHandler which in turn is defined (above) using the producer and topic.

        // we produce for 10 minutes and block the program(main thread) until then
        // Note: This means the program will produce continuously for 10 mins and then stop.
        // If we do not specify the block the app will crash when the main thread stops!!!
        TimeUnit.MINUTES.sleep(10);



    }
}
