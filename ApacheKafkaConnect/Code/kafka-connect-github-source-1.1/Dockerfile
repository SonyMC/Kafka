# Docker File to build a Docker image for Kafka and inject our application to it
FROM confluentinc/cp-kafka-connect:3.2.0

# Working Directory relative to the Dockerfile location in syour system
WORKDIR /kafka-connect-github-source-1.1
# Copy the 'config' and 'target folders
COPY config config
COPY target target

#Specify the Volumes in the Container
VOLUME /kafka-connect-github-source-1.1/config
VOLUME /kafka-connect-github-source-1.1/offsets

# - Start Kafka Connector in Standalone Mode
# - The config for the standalone worker node is available in 'worker.properties' file in D:\OneDrive\Study\DevOps\Kafka\ApacheKafkaConnect\Code\kafka-connect-github-source-1.1\config
# - The config for the GitHub connector is available in 'GitHubSourceConnectorExample.properties' file in D:\OneDrive\Study\DevOps\Kafka\ApacheKafkaConnect\Code\kafka-connect-github-source-1.1\config  
CMD CLASSPATH="$(find target/ -type f -name '*.jar'| grep '\-package' | tr '\n' ':')" connect-standalone config/worker.properties config/GitHubSourceConnectorExample.properties