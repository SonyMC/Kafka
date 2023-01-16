package com.mailsonymathew;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumerPart3 {


    // 1. Create OpenSearch Client

    /*
    High level REST client that wraps an instance of the low level RestClient and allows to build requests and read responses.
     */
    public static RestHighLevelClient createOpenSearchClient() {

        // Define Connection String to OpenSearch DB
        //String connString = "http://localhost:9200";
        // Replace Bonsai url in case you are using that...
        String connString = "https://za78wij468:u1bhfrwlmz@mailsonymathew-kafka-3660389779.us-east-1.bonsaisearch.net:443";

        // We build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST Client without Security( Basic Credentials)
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST Client with Security(Basic Credentials)
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }
        // Return restHighLevelClient to be used in the main thread
        return restHighLevelClient;
    }

    // 2. Create Kafka Client/Consumer
    private static KafkaConsumer<String, String> createKafkaConsumer() {

        String boostrapServers = "127.0.0.1:9092";  // bootstrap server
        String groupId = "consumer-opensearch-demo";

        // Note: We will specify the topic "wikimedia.recentchange" in teh main thread further below

        // Set Consumer Properties
        Properties properties = new Properties(); // create producer properties object
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, boostrapServers); // server property
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the key in the consumer message using a string serializer
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // De-Serialize(bytes into objects) the value in the consumer message using a string serializer
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId); // group-id
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // values = none/earliest/latest.  Here we have chosen to read from the latest entry

        // create and return the Kafka consumer
        return new KafkaConsumer<>(properties);

    }


    // 3. Main Code Logic
    public static void main(String[] args) throws IOException {

        Logger log = LoggerFactory.getLogger(OpenSearchConsumerPart3.class.getSimpleName());

        // first create an OpenSearch Client
        RestHighLevelClient openSearchClient = createOpenSearchClient();

        // create our Kafka Client accepting key.value pair as a string
        KafkaConsumer<String, String> consumer = createKafkaConsumer(); // this function is defined above


        //We need to create the Index on OpenSearch if it doesn't exist already
        // Note: Index is where the docs are stored in OpenSearch

        /*
        The OpenSearchClient and Kafka Consumer will automatically be closed on success/failure of the try block.
        Hence there is no need to explicitly close it.
         */
        try (openSearchClient; consumer) {  // pass in the openSearchClient & Kafka consumer as arguments in the try block
            // check if index 'wikimedia' exists in OpenSearch and return boolean value.
            boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);
            // if index 'wikimedia'  does not exist create the index
            if (!indexExists) {
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");  //Index name to be created is 'wikimedia'
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                log.info("The Wikimedia Index has been created!");
            } else {
                log.info("The Wikimedia Index already exits");
            }

            // we subscribe to the consumer
            // "wikimedia.recentchange" is the topic name for picking up the Wikimedia recent changes that we had defined in the producer demo
            consumer.subscribe(Collections.singleton("wikimedia.recentchange"));  // subscribe to the latest changes

            // consume the data that we have produced from wikimedia...
            while (true) {

                // Get Consumer Recs
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000)); // block for 3 secs

                // Track the record Count
                int recordCount = records.count();
                log.info("Received " + recordCount + " record(s)");

                // Now Send the Records picked from Kafka to OpenSearch
                // We can either send it one by one or in bulk
                // For this  demo we will be sending it in one by one which is also called a index request

                // Parse the Kafka Recs one by One and send to OpenSearch
                for (ConsumerRecord<String, String> record : records) {

                    // Strategy 1
                    // define an ID using Kafka Record coordinates
                    String id = record.topic() + "_" + record.partition() + "_" + record.offset();

                    try {  // Putting in a try catch to skip the concrete object exception which will happen otheewise
                        // Send the record one-by-one into OpenSearch
                        // Add the Wikimedia rec to the OpenSearch index "wikimedia"
                        IndexRequest indexRequest = new IndexRequest("wikimedia")
                                .source(record.value(), XContentType.JSON)
                                .id(id); // Pass the unique id to the index request


                        // Now access the openSearchClient to add the record
                        IndexResponse response = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);

                        log.info("******* Inserted one document into OpenSearch ******** = " + response.getId());
                    } catch (Exception e) {
                        // empty .
                    }


                } // for loop ends


            }   // while loop ends

        }

    }

    // main code logic

    // close things  - commented out because we have handled the openSearchClient close in the try block in main()
    // openSearchClient.close();


}
