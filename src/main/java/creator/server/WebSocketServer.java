package creator.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import creator.core.domain.OHLCV;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/start-websocket/{name}", encoders = OHLCVEncoder.class)
@ApplicationScoped
public class WebSocketServer {

	public Map<String, Session> sessions = new ConcurrentHashMap<>();

	@OnOpen
	public void onOpen(Session session, @PathParam("name") String name) {
		sessions.put(name, session);
		System.out.println("onOpen> " + name);
	}

	@OnClose
	public void onClose(Session session, @PathParam("name") String name) {
		sessions.remove(name, session);
		System.out.println("onClose> " + name);
	}

	@OnError
	public void onError(Session session, @PathParam("name") String name, Throwable throwable) {
		sessions.remove(name, session);
		System.out.println("onError> " + name + ": " + throwable);
	}

	@OnMessage
	public void onMessage(String message, @PathParam("name") String name) {
		System.out.println("onMessage> " + name + ": " + message);
	}

	@Incoming("requests")
	@Outgoing("quotes")
	public OHLCV broadcastAsync(OHLCV message) {
		sessions.values().forEach(s -> sendAsync(s, message));
		return message;
	}

	private void sendAsync(Session s, OHLCV message) {
		s.getAsyncRemote().sendObject(message, result -> {
			if (result.getException() != null) {
				System.out.println("Unable to send message: " + result.getException());
			}
		});
	}
}
