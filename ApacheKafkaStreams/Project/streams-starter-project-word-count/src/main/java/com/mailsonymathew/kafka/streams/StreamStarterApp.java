package com.mailsonymathew.kafka.streams;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

public class StreamStarterApp {
	
	
	public static void main(String[] args) {
		
		/*
		 *  Define configurations
		 */
        Properties config = new Properties();
        //Application ID
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-starter-app-word-count");
        // Bootstrap Server 
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // If our app is restarted or disconnected, read from the earlier data available
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Serde - Serialization and Deserialization for key
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // Serde - Serialization and Deserialization for value
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> kStream = builder.stream("input-topic-name");
        // do stuff
        kStream.to("word-count-output");

        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.cleanUp(); // only do this in dev - not in prod
        streams.start();

        // print the topology
        streams.metadataForLocalThreads().forEach(data -> System.out.println(data));
        
        // shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
		
	}

}
