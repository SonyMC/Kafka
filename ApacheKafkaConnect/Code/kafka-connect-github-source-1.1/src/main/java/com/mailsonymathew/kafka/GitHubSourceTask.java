package com.mailsonymathew.kafka;

import static com.mailsonymathew.kafka.GitHubSchemas.CREATED_AT_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.KEY_SCHEMA;
import static com.mailsonymathew.kafka.GitHubSchemas.NEXT_PAGE_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.NUMBER_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.OWNER_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.PR_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.PR_HTML_URL_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.PR_SCHEMA;
import static com.mailsonymathew.kafka.GitHubSchemas.PR_URL_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.REPOSITORY_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.STATE_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.TITLE_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.UPDATED_AT_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.URL_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.USER_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.USER_ID_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.USER_LOGIN_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.USER_SCHEMA;
import static com.mailsonymathew.kafka.GitHubSchemas.USER_URL_FIELD;
import static com.mailsonymathew.kafka.GitHubSchemas.VALUE_SCHEMA;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mailsonymathew.kafka.model.Issue;
import com.mailsonymathew.kafka.model.PullRequest;
import com.mailsonymathew.kafka.model.User;
import com.mailsonymathew.kafka.utils.DateUtils;

/* - This class does the actual work
 * - It's supposed to initialise, then find where to resume from , and finally poll the source for records
 * - Used to identify what partition and offset our Source Connector is using or has last used.
*/
public class GitHubSourceTask extends SourceTask {
	
	// Logger
    private static final Logger log = LoggerFactory.getLogger(GitHubSourceTask.class);
    
    // Connector Configuration
    public GitHubSourceConnectorConfig config;
    
    // Variables
    protected Instant nextQuerySince;
    protected Integer lastIssueNumber;
    protected Integer nextPageToVisit = 1;
    protected Instant lastUpdatedAt;
    
    // GitHubHttpAPIClient used to launch HTTP Get requests
    GitHubAPIHttpClient gitHubHttpAPIClient;
    
    
    // The Version 
    @Override
    public String version() {
        return VersionUtil.getVersion();
    }

    
    //Do things here that are required to start your task. This could be open a connection to a database, etc.
    @Override
    public void start(Map<String, String> map) {
    	// Initialise the config using the GitHubSourceConnectorConfig constructor
    	config = new GitHubSourceConnectorConfig(map);
    	// Where to resume from...
        initializeLastVariables();
        //Initialise the  GitHubHttpAPIClient used to launch HTTP Get requests
        gitHubHttpAPIClient = new GitHubAPIHttpClient(config);
    }
    
    
    // Returns from where to resume
    private void initializeLastVariables(){
    	// Initialise offset
        Map<String, Object> lastSourceOffset = null;
        // Retrieve the last offset for the sourcePartition 
        lastSourceOffset = context.offsetStorageReader().offset(sourcePartition());
        // If the Source Offset is null, iinitilaize to 7 days ago
        if( lastSourceOffset == null){
            // we haven't fetched anything yet, so we initialise to 7 days ago
            nextQuerySince = config.getSince();
            lastIssueNumber = -1;  // Since there is nothing
        } 
        // If the Source Offset is not null
        else {
        	//Get Offset details into Objects 
            Object updatedAt = lastSourceOffset.get(UPDATED_AT_FIELD);
            Object issueNumber = lastSourceOffset.get(NUMBER_FIELD);
            Object nextPage = lastSourceOffset.get(NEXT_PAGE_FIELD);
            
            // Parse and store the objecs into variables
            if(updatedAt != null && (updatedAt instanceof String)){
                nextQuerySince = Instant.parse((String) updatedAt);
            }
            if(issueNumber != null && (issueNumber instanceof String)){
                lastIssueNumber = Integer.valueOf((String) issueNumber);
            }
            if (nextPage != null && (nextPage instanceof String)){
                nextPageToVisit = Integer.valueOf((String) nextPage);
            }
        }
    }


    /*
     * Poll the Source record.
     * Since this function would poll the source indefinitely, we need to handle any exceptions due to Interruptions
     */
    @Override
    public List<SourceRecord> poll() throws InterruptedException {
    	
    	// Sleep if needed
        gitHubHttpAPIClient.sleepIfNeed();

        // fetch data
        // Source REcordfs will be returned as a List
        final ArrayList<SourceRecord> records = new ArrayList<>();
        
        // Get the response from the call
        JSONArray issues = gitHubHttpAPIClient.getNextIssues(nextPageToVisit, nextQuerySince);
        
        // We'll count how many results we get with i
        int i = 0;
        // For each issue...
        for (Object obj : issues) {
            // Create an Issue Object
        	Issue issue = Issue.fromJson((JSONObject) obj);
            // Generate a Source Record
        	SourceRecord sourceRecord = generateSourceRecord(issue);
            records.add(sourceRecord);
            i += 1;
            lastUpdatedAt = issue.getUpdatedAt();
        }
        
        if (i > 0) log.info(String.format("Fetched %s record(s)", i));
        if (i == 100){ //Page full
            // Pagination: we have reached a full batch, we need to get the next one
            nextPageToVisit += 1;
        }
        else { // PAge not full
            nextQuerySince = lastUpdatedAt.plusSeconds(1);
            nextPageToVisit = 1;
            gitHubHttpAPIClient.sleep();
        }
        return records;
    }
    
    // Generate the Source Record
    // Source Record is similar to a Producer Record with a few extra fields
    private SourceRecord generateSourceRecord(Issue issue) {
        return new SourceRecord(
                sourcePartition(), // function defined below 
                sourceOffset(issue.getUpdatedAt()), // function defined below 
                config.getTopic(), // topic to write to
                null, // partition will be inferred by the framework
                KEY_SCHEMA, //Key Schema
                buildRecordKey(issue), //build record key
                VALUE_SCHEMA, // Value Scheme
                buildRecordValue(issue), // build record value
                issue.getUpdatedAt().toEpochMilli());
    }

    @Override
    public void stop() {
        // Do whatever is required to stop your task.
    }
    
    
    // Return Map for Source Partition 
    private Map<String, String> sourcePartition() {
        Map<String, String> map = new HashMap<>();
        map.put(OWNER_FIELD, config.getOwnerConfig());
        map.put(REPOSITORY_FIELD, config.getRepoConfig());
        return map;
    }

    private Map<String, String> sourceOffset(Instant updatedAt) {
        Map<String, String> map = new HashMap<>();
        map.put(UPDATED_AT_FIELD, DateUtils.MaxInstant(updatedAt, nextQuerySince).toString());
        map.put(NEXT_PAGE_FIELD, nextPageToVisit.toString());
        return map;
    }
    
    // Function to build Record Key
    private Struct buildRecordKey(Issue issue){
        // Key Schema
        Struct key = new Struct(KEY_SCHEMA)
                .put(OWNER_FIELD, config.getOwnerConfig())
                .put(REPOSITORY_FIELD, config.getRepoConfig())
                .put(NUMBER_FIELD, issue.getNumber());

        return key;
    }
    
    
    // Function to build Record Value
    private Struct buildRecordValue(Issue issue){

        // Issue top level fields
        Struct valueStruct = new Struct(VALUE_SCHEMA)
                .put(URL_FIELD, issue.getUrl())
                .put(TITLE_FIELD, issue.getTitle())
                .put(CREATED_AT_FIELD, issue.getCreatedAt().toEpochMilli())
                .put(UPDATED_AT_FIELD, issue.getUpdatedAt().toEpochMilli())
                .put(NUMBER_FIELD, issue.getNumber())
                .put(STATE_FIELD, issue.getState());

        // User is mandatory
        User user = issue.getUser();
        Struct userStruct = new Struct(USER_SCHEMA)
                .put(USER_URL_FIELD, user.getUrl())
                .put(USER_ID_FIELD, user.getId())
                .put(USER_LOGIN_FIELD, user.getLogin());
        valueStruct.put(USER_FIELD, userStruct);

        // Pull request is optional
        PullRequest pullRequest = issue.getPullRequest();
        if (pullRequest != null) {
            Struct prStruct = new Struct(PR_SCHEMA)
                    .put(PR_URL_FIELD, pullRequest.getUrl())
                    .put(PR_HTML_URL_FIELD, pullRequest.getHtmlUrl());
            valueStruct.put(PR_FIELD, prStruct);
        }

        return valueStruct;
    }

}