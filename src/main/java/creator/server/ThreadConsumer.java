package creator.server;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import jakarta.websocket.Session;

public class ThreadConsumer implements Runnable { 

	private Consumer<String, String> kafkaConsumer;
	private ConcurrentLinkedDeque<Session> sessions = new ConcurrentLinkedDeque<>();
	private String bootstrapServers;
	private String groupId;
	private String topic;
	
	private boolean stopPoll = false;
	private boolean keepGoing = true;

	public ThreadConsumer(String bootstrapServers, String groupId, String topic,
			ConcurrentLinkedDeque<Session> sessions) {
		
		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "DYNAMIC" + "-" + topic);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		
		this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topic = topic;
        this.sessions = sessions;
        if (sessions.isEmpty()) {
        	stopPoll = true;
        }
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
        } catch (WakeupException e) { 
            System.out.println("Received shutdown signal"); 
        } finally { 
        	kafkaConsumer.close();
            System.out.println("kafkaConsumer closed"); 
        } 
    } 
	
	private void sendAsync(Session s, String message) {
		s.getAsyncRemote().sendObject(message, result -> {
			if (result.getException() != null) {
				System.out.println("Unable to send message: " + result.getException());
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
	public ConcurrentLinkedDeque<Session> getSessions() {
		return sessions;
	}
	public void addSession(Session session) {
		sessions.add(session);
	}		
	public void removeSession(Session session) {
		sessions.remove(session);
	}	
	public void updateTopic(String newTopic) {
		stopPoll = true;
		topic = newTopic;
		kafkaConsumer.unsubscribe();
		kafkaConsumer.subscribe(Collections.singletonList(topic));
		stopPoll = false;
	}

	public Consumer<String, String> getKafkaConsumer() {
		return kafkaConsumer;
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
