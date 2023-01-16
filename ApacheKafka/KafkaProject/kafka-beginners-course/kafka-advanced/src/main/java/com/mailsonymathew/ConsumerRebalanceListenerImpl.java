package com.mailsonymathew;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
Creates a Listener and keeps track of how far we have been consuming in our Kafka topic partitions.
Note : The Listener creation is defined in 'ConsumerDemoRebalanceListener'
 */
public class ConsumerRebalanceListenerImpl implements ConsumerRebalanceListener  {

    // Logger definition
    private static final Logger log = LoggerFactory.getLogger(ConsumerRebalanceListenerImpl.class);
    // Consumer Declaration
    private KafkaConsumer<String, String> consumer;
    // Hashmap to keeo offsets
    private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    // Constructor
    public ConsumerRebalanceListenerImpl(KafkaConsumer<String, String> consumer) {
        this.consumer = consumer;
    }

    // Populate the Hashmap 'currentOffsets'
    // We track internally in the class the offsets of how far we have consumers using currentOffsets
    // In the function addOffsetToTrack we make sure to increment the offset by 1 in order to commit the position properly
    public void addOffsetToTrack(String topic, int partition, long offset){
        currentOffsets.put(
                new TopicPartition(topic, partition),
                new OffsetAndMetadata(offset + 1, null));  // Increment the offset  value by one before storing . Metadata is null.
    }

    // Overridden method for when partition is revoked from 'ConsumerRebalanceListener' implementation
    // We use a synchronous consumer.commitSync call in onPartitionsRevoked to block until the offsets are successfully committed
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            log.info("onPartitionsRevoked callback triggered");
            log.info("Committing offsets: " + currentOffsets);

            consumer.commitSync(currentOffsets);    // commit the offsets synchronously
    }

    // Overridden method for when Partition is assigned from 'ConsumerRebalanceListener' implementation
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("onPartitionsAssigned callback triggered");
    }

    // This is used when we shut down our consumer gracefully
    public Map<TopicPartition, OffsetAndMetadata> getCurrentOffsets() {
        return currentOffsets;
    }
}
