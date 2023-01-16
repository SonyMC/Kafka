package com.mailsonymathew;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class OpenSearchConsumerPart1 {


    // 1. Create OpenSearch Client

    /*
    High level REST client that wraps an instance of the low level RestClient and allows to build requests and read responses.
     */
    public static RestHighLevelClient createOpenSearchClient() {

        // Define Connection String to OpenSearch DB
        // String connString = "http://localhost:9200";
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


    // 3. Main Code Logic
    public static void main(String[] args) throws IOException {

        Logger log = LoggerFactory.getLogger(OpenSearchConsumerPart1.class.getSimpleName());

        // first create an OpenSearch Client
        RestHighLevelClient openSearchClient = createOpenSearchClient();


        //We need to create the Index on OpenSearch if it doesn't exist already
        // Note: Index is where the docs are stored in OpenSearch

        /*
        The OpenSearchClient will automatically be closed on success/failure of the try block.
        Hence there is no need to explicitly close it.
         */
        try (openSearchClient) {  // pass in the openSearchClient & Kafka consumer as arguments in the try block
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


        }


        // main code logic

        // close things  - commented out because we have handled the openSearchClient close in the try block in main()
        // openSearchClient.close();
    }

}
