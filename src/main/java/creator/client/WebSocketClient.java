package creator.client;

import java.net.URI;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint
@ApplicationScoped
public class WebSocketClient {
	
	private final URI uri = URI.create("ws://localhost:8080/start-websocket/EURGBP");
	private final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
	private Session session;
	   
	@Scheduled(every = "10s")
	void init() { 
		try {
			if (session == null) {
				session = wsContainer.connectToServer(this, uri);
			}
		} catch (Exception e) {
			System.out.println("ERROR CLIENT: " + e);
		}
	}
	   
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("CLIENT: onOpen> " + session.toString());
        session.getAsyncRemote().sendText("_ready_");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("CLIENT: OnMessage> " + message);
    }
    
}
