package com.mailsonymathew;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    public class WikimediaChangeHandler implements EventHandler {
    private final Logger log = LoggerFactory.getLogger(WikimediaChangeHandler.class.getSimpleName());

    KafkaProducer<String, String> kafkaProducer;
    String topic;


    // We are defining a constructor so that  when the WikimediaChangeHandler object is created it gets the producer & topic as an argument.
    public WikimediaChangeHandler(KafkaProducer<String, String> kafkaProducer, String topic){
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    @Override
    public void onOpen()  {
        // nothing to be done when the stream is opened
    }

    @Override
    public void onClosed()  {
        kafkaProducer.close();   // Close the producer when the Stream is closed
    }

    @Override
    public void onMessage(String event, MessageEvent messageEvent) throws Exception {
        //  Stream has received a message
        log.info(messageEvent.getData());
        // asynchronous
        // Extract the message data using messageEvent.getData()) and send message to Producer against the specified topic
        kafkaProducer.send(new ProducerRecord<>(topic, messageEvent.getData()));  // The messageEvent.getData() returns a String
    }

    @Override
    public void onComment(String comment)  {
           // nothing here
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error in Stream Reading", t);

    }
}
