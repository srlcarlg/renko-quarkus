package creator.server;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jboss.logging.Logger;

import jakarta.websocket.Session;

public class ThreadConsumer implements Runnable { 

	private Consumer<String, String> kafkaConsumer;
	private ConcurrentLinkedDeque<Session> sessions = new ConcurrentLinkedDeque<>();
	private String bootstrapServers;
	private String groupId;
	private String topic;
	private boolean stopPoll = true;
	private boolean keepGoing = true;
    private static final Logger LOG = Logger.getLogger(ThreadConsumer.class);

	public ThreadConsumer(String bootstrapServers, String groupId, String topic) {
		
		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "DYNAMIC-" + topic);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		
		this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topic = topic;
        sessions = new ConcurrentLinkedDeque<>();
        
		kafkaConsumer = new KafkaConsumer<>(properties);
	}
	
	@Override
    public void run() { 
        try { 
            // Poll the data 
            while (keepGoing) { 
            	if (stopPoll || sessions.isEmpty()) {
            		continue;
            	}
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000)); 
                records.forEach(infoRecord ->
                	sessions.parallelStream().forEach(session ->
                		sendAsync(session, infoRecord.value())
                	)
                );
                kafkaConsumer.commitAsync();
            } 
        } catch (Exception e) {
        	
        } finally { 
        	kafkaConsumer.close();
        } 
    } 
	
	private void sendAsync(Session s, String message) {
		s.getAsyncRemote().sendObject(message, result -> {
			if (result.getException() != null) {
				LOG.error(String.format(
					"Unable to send message from topic %s: %s", topic, result.getException()));
			}
		});
	}
	
    public void shutDown() { 
    	keepGoing = false;
    	kafkaConsumer.wakeup(); 
	}
    
	public void subscribeToTopic(String newTopic) {
		stopPoll = true;
		topic = newTopic;
		kafkaConsumer.subscribe(Collections.singletonList(topic));
		stopPoll = false;
	}
	public void updateTopic(String newTopic) {
		stopPoll = true;
		topic = newTopic;
		kafkaConsumer.unsubscribe();
		kafkaConsumer.subscribe(Collections.singletonList(topic));
		stopPoll = false;
	}
	public void addSession(Session session) {
		sessions.add(session);
	}		
	public void removeSession(Session session) {
		sessions.remove(session);
	}
	
	public Consumer<String, String> getKafkaConsumer() {
		return kafkaConsumer;
	}
	public ConcurrentLinkedDeque<Session> getSessions() {
		return sessions;
	}
	public String getBootstrapServers() {
		return bootstrapServers;
	}
	public String getGroupId() {
		return groupId;
	}
	public String getTopic() {
		return topic;
	}

	public void setKafkaConsumer(Consumer<String, String> kafkaConsumer) {
		this.kafkaConsumer = kafkaConsumer;
	}
	public void setSessions(ConcurrentLinkedDeque<Session> sessions) {
		this.sessions = sessions;
	}
	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}	
}
