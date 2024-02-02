package creator.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@QuarkusTest
@TestInstance(Lifecycle.PER_CLASS)
public class ConsumersTest {
	
	private static Map<String, Session> sessions = new ConcurrentHashMap<>();

    @TestHTTPResource("/renko")
    String strURI;
    
	@Inject
	WebSocketServer server;
	
    @ParameterizedTest
    @MethodSource("symbolsInfoProvider")
    @DisplayName("Consumers and topics should be changed/created dynamically")
    void ConsumersTopicsShouldBe_CreatedChanged_Dynamically(String symbol, double brickSize, List<String> modes) {
    	
    	if (!symbol.equals("US500")) {
	    	modes.forEach(mode -> {
	        	URI modeURI = getURIwithMode(symbol, mode);
	    		String topicName = getTopicName(symbol, mode);
	    		try { 
	    			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	    			sessions.put(topicName, container.connectToServer(Client.class, modeURI));
	            } catch (Exception e) {}
	    	});
    	} else {
    		// At this point:
    		// 3 static consumers (of each mode) already have a topic (EURGBP),
    		// 3 dynamic consumers (of each mode) created with the needed topic(US30)
    		modes.forEach(mode -> {
	        	URI modeURI = getURIwithMode(symbol, mode);
        		String topicName = getTopicName(symbol, mode);
        		switch (mode) {
				case "normal":
	    			// Remove session of STATIC consumer of group NORMAL 
					stopSessionCreateAnother("us30_normal", topicName, modeURI); break;
				case "wicks": 
					// Remove session of DYNAMIC consumer of group WICKS
					stopSessionCreateAnother("eurgbp_wicks", topicName, modeURI); break;
				default:
		    		// Remove session of DYNAMIC consumer of group NONGAP
					stopSessionCreateAnother("eurgbp_nongap", topicName, modeURI); break;
				}
    		});
    	}
    	
        try { Thread.sleep(1000); } catch (Exception ex) { }

    	List<String> staticConsumersTopics = server.getStaticConsumers()
    			.parallelStream().map(ThreadConsumer::getTopic).toList();
    	List<String> dynamicConsurmersTopics = server.getDynamicConsumers().values()
    			.parallelStream().map(ThreadConsumer::getTopic).toList();
    	
    	assertTrue(sessions.keySet().containsAll(staticConsumersTopics));
    	assertTrue(sessions.keySet().containsAll(dynamicConsurmersTopics));
    }

	private URI getURIwithMode(String symbol, String mode) {
		return URI.create(String.format("%s/%s/%s",strURI, symbol, mode));
	}

	private String getTopicName(String symbol, String mode) {
		return symbol.toLowerCase() + "_" + mode;
	}
    
	void stopSessionCreateAnother(String keyToClose, String keyToOpen, URI uri) {
		try {
			sessions.get(keyToClose).close();
			sessions.remove(keyToClose);
    		// Add/replace topic to this empty session dynamic/static consumer
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			sessions.put(keyToOpen, container.connectToServer(Client.class, uri));
		} catch (Exception e) {}		
	}
	
	static Stream<Arguments> symbolsInfoProvider() {
	    List<String> mode = Arrays.asList("normal", "wicks", "nongap");
		return Stream.of(
	    	Arguments.of("US30", 5D, mode),
	        Arguments.of("EURGBP", 0.0003, mode),
	        Arguments.of("US500", 12D, mode)
	    );
	}
	
	@ClientEndpoint
    public static class Client {
        @OnOpen
        public void open(Session session) {
            // Send a message to indicate that we are ready,
            // as the message handler may not be registered immediately after this callback.
            session.getAsyncRemote().sendText("_ready_");
        }
    }
}
