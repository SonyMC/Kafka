package com.mailsonymathew.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class GitHubSourceConnector extends SourceConnector {
    /*
     * Logger Definition
     */
	//private static Logger log = LoggerFactory.getLogger(GitHubSourceConnector.class);
    
	/*
	 * Configuration Definition
	 */
	private GitHubSourceConnectorConfig config;
	
	/*
	 * Return the Version
	 */
    @Override
    public String version() {
        return VersionUtil.getVersion();
    }

    /*
     * Create connector configuration when you start your GitHub Source Connector
     * 
     */
    @Override
    public void start(Map<String, String> map) {
    	// The Input to the function is a map with a key. value pair 
    	// The map (key, values) is created using the configuration parameters provided. 
    	// config/GitHubSourceConnectorExample.properties : Contains sample configuration parms which can be referenced to understand how the map is formed. 
    	config = new GitHubSourceConnectorConfig(map);  
    }
    
    /*
     * Return the class of the task
     */
    @Override
    public Class<? extends Task> taskClass() {
        return GitHubSourceTask.class;
    }

    /*
     * Define the individual task configurations that will be executed.
     * Note: This particular Source Connector cannot be parallelised and can have only a max. of 1 task. So regardless of the task no. passed, it will return the details of 1 task.
     */
    @Override
    public List<Map<String, String>> taskConfigs(int i) {
    	// Create a config Array with initial capacity capacity 1.
        ArrayList<Map<String, String>> configs = new ArrayList<>(1);
        // Extract the map(key,value) used to create the config 
        configs.add(config.originalsStrings());
        return configs;
    }
    
    /*
     *  Do things that are necessary to stop your connector.
     *  Nothing is necessary to stop for this connector as we do not have any DB connections etc. that needs to be closed.
     */
    @Override
    public void stop() {
    }

    /*
     * Return the configuration used for the connector 
     */
    @Override
    public ConfigDef config() {
        return GitHubSourceConnectorConfig.conf();
    }
}
