package com.mailsonymathew.kafka.streams;

import java.time.Instant;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.state.KeyValueStore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/*
Functionality:
		- Read one topic from Kafka(KStream)
		- GroupByKey, because your topic already has the right key! - no repartition happens
		- Aggregate, to compute the "bank balance"

 */
public class BankBalanceExactlyOnceApp {

    public static void main(String[] args) {
        Properties config = new Properties();

        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "bank-balance-application");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // we disable the cache to demonstrate all the "steps" involved in the transformation - not recommended in prod
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");

        // Exactly once processing!!
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);

        // json Serde for Serializer and Deserializer
        final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

        StreamsBuilder builder = new StreamsBuilder();


        KStream<String, JsonNode> bankTransactions = builder.stream("bank-transactions",
                Consumed.with(Serdes.String(), jsonSerde));   // topic, consumed.with(key,value)

        // create the initial json object for balances
        ObjectNode initialBalance = JsonNodeFactory.instance.objectNode();
        initialBalance.put("count", 0);
        initialBalance.put("balance", 0);
        initialBalance.put("time", Instant.ofEpochMilli(0L).toString());

        // Kafka Streams Topology
        KTable<String, JsonNode> bankBalance = bankTransactions
                .groupByKey(Serialized.with(Serdes.String(), jsonSerde))  // - Group By Key using Key,Value
                .aggregate(   // - Aggregate using 4 parameters : 1. Initial Value, 2. Aggregator, 3. Store Name, 4. Aggregator Serde
                        () -> initialBalance,  // 1. Initial Value : Function taking no arguments and returning the initialBalance json object we had created above
                        (key, transaction, balance) -> newBalance(transaction, balance), // 2. Aggregator : Takes key, current transaction( viz. value) and  balance & computes new balance(using current transaction & old balance)
                        Materialized.<String, JsonNode, KeyValueStore<Bytes, byte[]>>as("bank-balance-agg") // 3. Store Name : Name the aggregator
                                .withKeySerde(Serdes.String()) // 4.Aggregator Serde
                                .withValueSerde(jsonSerde)
                );

        // Write the resulting topology back to Kafka to topic "bank-balance-exactly-once"
        bankBalance.toStream().to("bank-balance-exactly-once", Produced.with(Serdes.String(), jsonSerde)); // Produced.with(key serializer, value serializer)

        // Build Kafka Streams object using builder and config
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.cleanUp();  // cleanup
        streams.start();  // start

        // print the topology
        streams.localThreadsMetadata().forEach(data -> System.out.println(data));

        // shutdown hook to correctly close the streams application
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    /*
    - Aggregator function
     */
    private static JsonNode newBalance(JsonNode transaction, JsonNode balance) { // Input parms are Current Transaction and Old Balance
        // create a new balance json object
        ObjectNode newBalance = JsonNodeFactory.instance.objectNode();
        newBalance.put("count", balance.get("count").asInt() + 1); // old count from previous balance + 1
        newBalance.put("balance", balance.get("balance").asInt() + transaction.get("amount").asInt()); // balance = previous balance + amount in current transaction

        // Time calculation
        Long balanceEpoch = Instant.parse(balance.get("time").asText()).toEpochMilli();
        Long transactionEpoch = Instant.parse(transaction.get("time").asText()).toEpochMilli();
        Instant newBalanceInstant = Instant.ofEpochMilli(Math.max(balanceEpoch, transactionEpoch));
        newBalance.put("time", newBalanceInstant.toString());

        // Return newBalance as JsonNode
        return newBalance;
    }
}
