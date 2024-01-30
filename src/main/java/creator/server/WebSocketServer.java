package creator.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/renko/{symbol}/{mode}", encoders = OHLCVEncoder.class)
@ApplicationScoped
public class WebSocketServer {
    private static final Logger LOG = Logger.getLogger(WebSocketServer.class);
	
	private static final List<String> renkoModes = Arrays.asList("normal","wicks","nongap");
	// Static consumers for dynamic topics
	private static ConcurrentLinkedDeque<ThreadConsumer> staticConsumers = new ConcurrentLinkedDeque<>();
	// Dynamic consumers for dynamic topics (created as needed)
	private static Map<String, ThreadConsumer> consumers = new ConcurrentHashMap<>();
	
	@ConfigProperty(name = "kafka.bootstrap.servers")
	private String kafkaUrl;

	/**
	 *  Initiate static consumes with artificial topic (renko mode). <br>
	 *  We'll use the renko mode to identify the Group_ID of each consumer. <br>
	 *	No subscribe.
	 * @param ev
	 */
    void onStart(@Observes StartupEvent ev) {
		renkoModes.parallelStream().forEach(modeLoop -> {
			String groupId = modeLoop + "-in-memory";
			
			ThreadConsumer kafkaConsumer = new ThreadConsumer(kafkaUrl, groupId, modeLoop.toUpperCase());
			staticConsumers.add(kafkaConsumer);
			
			Thread thread = new Thread(kafkaConsumer);
			thread.start();
		});
	}
    void onShutdown(@Observes ShutdownEvent ev) {
		staticConsumers.parallelStream().forEach(ThreadConsumer::shutDown);
		consumers.values().parallelStream().forEach(ThreadConsumer::shutDown);
	}

	@OnOpen
	public void onOpen(Session session, @PathParam("symbol") String symbol, @PathParam("mode") String mode) {
		String topicName = getTopicName(symbol, mode);
		consumersLogic(topicName, mode, session);
		LOG.info(String.format("onOpen> %s", topicName));
	}

	@OnClose
	public void onClose(Session session, @PathParam("symbol") String symbol, @PathParam("mode") String mode) {
		removeSessionFromConsumer(session, symbol, mode);
		LOG.info(String.format("onClose> %s", getTopicName(symbol, mode)));
	}

	@OnError
	public void onError(Session session, @PathParam("symbol") String symbol, @PathParam("mode") String mode, Throwable throwable) throws Throwable {
		removeSessionFromConsumer(session, symbol, mode);
		LOG.error(String.format("onError> %s", getTopicName(symbol, mode)));
	}

	private void removeSessionFromConsumer(Session session, String symbol, String mode) {
		String topicName = getTopicName(symbol, mode);
		staticConsumers.forEach(consumer -> {
			if (consumer.getTopic().equals(topicName)) {
				consumer.removeSession(session);
			}
		});
		if (consumers.containsKey(topicName)) {
			consumers.get(topicName).removeSession(session);
		}
	}
	
	private void consumersLogic(String topicName, String mode, Session session) {
		// Static consumers first
		staticConsumers.parallelStream().forEach(consumer -> {
			String topicLoop = consumer.getTopic();
			if (topicLoop.equals(mode.toUpperCase())) {
				// First Connection for any symbol+mode
				consumer.addSession(session);
				consumer.subscribeToTopic(topicName);
				LOG.info(String.format(
					"First Connection to %s using static consumer of group %s",
					topicName, mode.toUpperCase()));
			} else if (topicLoop.equals(topicName)) {
				// Already have this symbol+mode
				consumer.addSession(session);
				LOG.info(String.format("New session added to static consumer with topic %s", topicName));
			} else {
				// Subscribe to a symbol+mode using an existing empty session consumer 
				// with the same Group_ID (renko mode)
				boolean isNotFirstConnect = renkoModes.stream()
						.noneMatch(x -> x.toUpperCase().equals(topicLoop));
				if (isNotFirstConnect) {
					String modeLoop = topicLoop.split("_")[1];
					if (modeLoop.equals(mode) && consumer.getSessions().isEmpty()) {
						consumer.updateTopic(topicName);
						consumer.addSession(session);
						LOG.info(String.format(
							"Using an existing empty session static consumer of group %s to topic %s", 
							modeLoop.toUpperCase(), topicName));
					}
				}
			}
		});
		
		// This symbol+mode doesn't have an existing consumer
		// and/or staticConsumers are already busy with others symbols.
		// So, create a new consumer specifically for it,
		// or use a existing consumer without session.
		boolean itsNotInStatic = staticConsumers.parallelStream()
				.map(ThreadConsumer::getTopic)
				.noneMatch(topic -> topic.equals(topicName));

		if (itsNotInStatic) {
			toDynamicConsumers(topicName, mode, session);
		}
	}
	
	private void toDynamicConsumers(String topicName, String mode, Session session) {
		if (consumers.containsKey(topicName)) {
			// For multiples sessions with the same symbol+mode
			consumers.get(topicName).addSession(session);
			LOG.info(String.format("New session added to dynamic consumer with topic %s", topicName));
		} else {
			// Check for a empty session consumer with the same Group_ID (renko mode)
			// then, update the topic, add session, update key.
			consumers.keySet().parallelStream().forEach(topicLoop -> {
				String modeLoop = topicLoop.split("_")[1];
				ThreadConsumer consumer = consumers.get(topicLoop);
				if (modeLoop.equals(mode) && consumer.getSessions().isEmpty()) {
					LOG.info(String.format(
						"Using an existing empty session dynamic consumer of group %s to topic %s",
						mode.toUpperCase(), topicName));
					consumers.get(topicLoop).updateTopic(topicName);
					consumers.get(topicLoop).addSession(session);
					consumers.put(topicName, consumers.remove(topicLoop));
				}
			});
			// else, create new consumer.
			if (!consumers.containsKey(topicName)) {
				createNewConsumerAndSubscribe(session, topicName);
			}
		}
	}
	
	private void createNewConsumerAndSubscribe(Session session, String topicName) {
		LOG.info(String.format("Creating new dynamic comsumer for %s", topicName));
		
		String mode = topicName.split("_")[1];
		String groupId = mode + "-in-memory";

		ThreadConsumer kafkaConsumer = new ThreadConsumer(kafkaUrl, groupId, mode.toUpperCase());
		kafkaConsumer.subscribeToTopic(topicName);
		kafkaConsumer.addSession(session);

		consumers.computeIfAbsent(topicName, key -> kafkaConsumer);

		Thread thread = new Thread(kafkaConsumer);
		thread.start();
	}

	private String getTopicName(String symbol, String mode) {
		return symbol.toLowerCase() + "_" + mode;
	}
	public ConcurrentLinkedDeque<ThreadConsumer> getStaticConsumers() {
		return staticConsumers;
	}	
	public Map<String, ThreadConsumer> getDynamicConsumers() {
		return consumers;
	}	
}
