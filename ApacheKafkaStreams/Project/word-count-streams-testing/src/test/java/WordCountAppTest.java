import com.mailsonymathew.kafka.streams.WordCountApp;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
/*
Since Kafka 2.4, the below two methods are deprecated
 */
import org.apache.kafka.streams.test.ConsumerRecordFactory;   //https://kafka.apache.org/25/javadoc/org/apache/kafka/streams/test/ConsumerRecordFactory.html
import org.apache.kafka.streams.test.OutputVerifier;  //https://kafka.apache.org/28/javadoc/org/apache/kafka/streams/test/OutputVerifier.html

/*
To do : Instead of teh above 2 imports, use the following
 */
//import org.apache.kafka.streams.TestInputTopic;  //https://kafka.apache.org/25/javadoc/org/apache/kafka/streams/TestInputTopic.html
//import org.apache.kafka.streams.TestOutputTopic;  //https://kafka.apache.org/26/javadoc/org/apache/kafka/streams/TestOutputTopic.html
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.test.OutputVerifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class WordCountAppTest {

    /*
    Simulates Kafka Environment
     */
    TopologyTestDriver testDriver;

    /*
    String Serializer for Key, Value
     */
    StringSerializer stringSerializer = new StringSerializer();

    /*
    Consumer Record Factory
     */
    ConsumerRecordFactory<String, String> recordFactory =
            new ConsumerRecordFactory<>(stringSerializer, stringSerializer);  // stringSerializer is used for key.value


    @Before // Before every test run
    public void setUpTopologyTestDriver(){
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // Create App Instance
        WordCountApp wordCountApp = new WordCountApp();
        // Create topology
        Topology topology = wordCountApp.createTopology();
        testDriver = new TopologyTestDriver(topology,config);
    }

    @After  // After the test
    public void closeTestDriver(){
        testDriver.close();
    }

    /*
    Push record into record Factory using the topic, key and value
     */
    public void pushNewInputRecord(String value){
        testDriver.pipeInput(recordFactory.create("word-count-input", null, value));
    }

    @Test
    public void dummyTest(){
        String dummy = "Du" + "mmy";
        assertEquals(dummy, "Dummy");
    }

    /*
    Read Output from Topic returning a ProducerRecord<String, Long>
     */
    public ProducerRecord<String, Long> readOutput(){
        return testDriver.readOutput("word-count-output", new StringDeserializer(), new LongDeserializer());
    }

    @Test
    public void makeSureCountsAreCorrect(){
        String firstExample = "testing Kafka Streams";
        // Push input Record
        pushNewInputRecord(firstExample);
        OutputVerifier.compareKeyValue(readOutput(), "testing", 1L);  // 'testing' occurs once in Kafka Stream based on input
        OutputVerifier.compareKeyValue(readOutput(), "kafka", 1L); // 'kafka' occurs once in Kafka Stream based on input
        OutputVerifier.compareKeyValue(readOutput(), "streams", 1L); // 'streams' occurs once in Kafka Stream based on input
        assertEquals(readOutput(), null);

        String secondExample = "testing Kafka again";
        pushNewInputRecord(secondExample);
        OutputVerifier.compareKeyValue(readOutput(), "testing", 2L); // 'testing' occurs now twice in Kafka Stream based on input
        OutputVerifier.compareKeyValue(readOutput(), "kafka", 2L);  // 'kafka' occurs now twice in Kafka Stream based on input
        OutputVerifier.compareKeyValue(readOutput(), "again", 1L); // 'again' occurs once in Kafka Stream based on input

    }

    @Test
    public void makeSureWordsBecomeLowercase(){
        String upperCaseString = "KAFKA kafka Kafka";
        pushNewInputRecord(upperCaseString);
        OutputVerifier.compareKeyValue(readOutput(), "kafka", 1L);
        OutputVerifier.compareKeyValue(readOutput(), "kafka", 2L);
        OutputVerifier.compareKeyValue(readOutput(), "kafka", 3L);

    }
}
