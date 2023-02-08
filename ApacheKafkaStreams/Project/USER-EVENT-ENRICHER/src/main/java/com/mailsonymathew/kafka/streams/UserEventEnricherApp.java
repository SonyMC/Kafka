package com.mailsonymathew.kafka.streams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class UserEventEnricherApp {
    public static void main(String[] args) {

        /*
        - Create Properties
         */
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "user-event-enricher-app");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // Stream Builder
        StreamsBuilder builder = new StreamsBuilder();

        // Global Table: we get a global table out of Kafka. This table will be replicated on each Kafka Streams application
        // the key of our globalKTable is the user ID
        // Note: There is no need to specify the serdes as we have already done tht in the stream properties setup
        GlobalKTable<String, String> usersGlobalTable = builder.globalTable("user-table");

        // We get a stream of user purchases
        KStream<String, String> userPurchases = builder.stream("user-purchases");

        // We want to enrich the userPurchases Steam wih user information from the global table.
        // Create an Inner join ( i.e. data will exist on both LHS and RHS)
        KStream<String, String> userPurchasesEnrichedJoin =
                userPurchases.join(usersGlobalTable,  // Inner  Join
                        (key, value) -> key, /* map from the (key, value) of this stream to the key of the GlobalKTable */
                        (userPurchase, userInfo) -> "Purchase=" + userPurchase + ",UserInfo=[" + userInfo + "]"  // userPurchase is the user purchase stream while the userInfo is the data from the global table
                );

        // Write the enriched inner join KStream to the topic 'user-purchases-enriched-inner-join'
        userPurchasesEnrichedJoin.to("user-purchases-enriched-inner-join");

        // we want to enrich that stream using a Left Join
        KStream<String, String> userPurchasesEnrichedLeftJoin =
                userPurchases.leftJoin(usersGlobalTable,
                        (key, value) -> key, /* map from the (key, value) of this stream to the key of the GlobalKTable */
                        (userPurchase, userInfo) -> {
                            // as this is a left join, userInfo can be null
                            if (userInfo != null) {
                                return "Purchase=" + userPurchase + ",UserInfo=[" + userInfo + "]";
                            } else {
                                return "Purchase=" + userPurchase + ",UserInfo=null";
                            }
                        }
                );

        // Write the enriched left join KStream to the topic 'user-purchases-enriched-left-join'
        userPurchasesEnrichedLeftJoin.to("user-purchases-enriched-left-join");


        // Build Kafka Streams application and start its
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.cleanUp(); // only do this in dev - not in prod
        streams.start();

        // print the topology
        streams.localThreadsMetadata().forEach(data -> System.out.println(data));

        // shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }
}
