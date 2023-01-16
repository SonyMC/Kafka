package com.mailsonymathew.kafka;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.kafka.connect.errors.ConnectException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

// GitHubHttpAPIClient used to launch HTTP Get requests
public class GitHubAPIHttpClient {
	
	//Logger
    private static final Logger log = LoggerFactory.getLogger(GitHubAPIHttpClient.class);

    // Rate Limiting variables - for efficient http requests
    private Integer XRateLimit = 9999;
    private Integer XRateRemaining = 9999;
    private long XRateReset = Instant.MAX.getEpochSecond();

    // The Source Connector COnfiguration	
    GitHubSourceConnectorConfig config;

    // Main Constructor for GitHubAPIHttpClient 
    public GitHubAPIHttpClient(GitHubSourceConnectorConfig config){
        this.config = config;
    }
    
    // Get Next Issues
    protected JSONArray getNextIssues(Integer page, Instant since) throws InterruptedException {
    		
        HttpResponse<JsonNode> jsonResponse;
        try {
        	// Get the JSON Response which can be a success or a failure
            jsonResponse = getNextIssuesAPI(page, since);
            
            // Deal with headers in any case
            Headers headers = jsonResponse.getHeaders();
            XRateLimit = Integer.valueOf(headers.getFirst("X-RateLimit-Limit"));
            XRateRemaining = Integer.valueOf(headers.getFirst("X-RateLimit-Remaining"));
            XRateReset = Integer.valueOf(headers.getFirst("X-RateLimit-Reset"));
            
            // Process based on REsponse Code
            switch (jsonResponse.getStatus()){
                case 200: // Success - Return Response Body in an array 
                    return jsonResponse.getBody().getArray();
                case 401: // GitHub Authentication Error
                    throw new ConnectException("Bad GitHub credentials provided, please edit your config");
                case 403: // Oops...we have hit the Request Rate Limit Ceiling 
                    // We have issued too many requests.Let us put this thread to sleep till we can issue next request
                    // Log info
                	log.info(jsonResponse.getBody().getObject().getString("message"));
                    log.info(String.format("Your rate limit is %s", XRateLimit));
                    log.info(String.format("Your remaining calls is %s", XRateRemaining));
                    // How much more time must we wait before sending a new request
                    log.info(String.format("The limit will reset at %s",
                            LocalDateTime.ofInstant(Instant.ofEpochSecond(XRateReset), ZoneOffset.systemDefault())));
                    
                    // Calculate sleep time based on value provided in Header and current time 
                    long sleepTime = XRateReset - Instant.now().getEpochSecond(); 
                    
                    // Log how much time we are going to put this baby to sleep
                    log.info(String.format("Sleeping for %s seconds", sleepTime ));
                    
                    // Sleep command issued based on calculated Sleep Time
                    Thread.sleep(1000 * sleepTime);
                    
                    // Recursive call to get the next response
                    return getNextIssues(page, since);
                
                    // If no matches are found in the switch-case log values and sleep for 5 secs:
                default:
                    log.error(constructUrl(page, since));
                    log.error(String.valueOf(jsonResponse.getStatus()));
                    log.error(jsonResponse.getBody().toString());
                    log.error(jsonResponse.getHeaders().toString());
                    log.error("Unknown error: Sleeping 5 seconds " +
                            "before re-trying");
                    Thread.sleep(5000L);
                    
                 // Recursive call to get the next response
                    return getNextIssues(page, since);
            }
        } catch (UnirestException e) { 
            e.printStackTrace();
            Thread.sleep(5000L); // Sleep for 5 secs
            return new JSONArray();
        }
    }
    
    // Call the API using the Unirest HTTP library
    protected HttpResponse<JsonNode> getNextIssuesAPI(Integer page, Instant since) throws UnirestException {
        // Using Unirest libraries , construct an url
    	GetRequest unirest = Unirest.get(constructUrl(page, since));
        // Provide Username and Password from BAsic Authentication Headers
    	if (!config.getAuthUsername().isEmpty() && !config.getAuthPassword().isEmpty() ){
            unirest = unirest.basicAuth(config.getAuthUsername(), config.getAuthPassword());
        }
        log.debug(String.format("GET %s", unirest.getUrl()));
        
        return unirest.asJson();
    }
    
    // Construct the GitHub Issues url for a particular repo
    protected String constructUrl(Integer page, Instant since){
    	/*
    	 * Note : All '%s' parameters in the url below will get replaced by the following config values in the provided order 
    	 */
        return String.format(
                "https://api.github.com/repos/%s/%s/issues?page=%s&per_page=%s&since=%s&state=all&direction=asc&sort=updated",
                config.getOwnerConfig(),
                config.getRepoConfig(),
                page,
                config.getBatchSize(),
                since.toString());
    }
    
    
    // Sleep function for the GitHub API client( i.e this class)
    public void sleep() throws InterruptedException {
        long sleepTime = (long) Math.ceil(
                (double) (XRateReset - Instant.now().getEpochSecond()) / XRateRemaining);
        log.debug(String.format("Sleeping for %s seconds", sleepTime ));
        Thread.sleep(1000 * sleepTime);
    }

    
    // Sleep function which can be used before the Rate Limit is hit.
    public void sleepIfNeed() throws InterruptedException {
        // Sleep if needed
        if (XRateRemaining <= 10 && XRateRemaining > 0) {
            log.info(String.format("Approaching limit soon, you have %s requests left", XRateRemaining));
            sleep();
        }
    }
}