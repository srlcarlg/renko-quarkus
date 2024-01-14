package creator.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import creator.core.domain.OHLCV;
import creator.core.domain.TopicMsg;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/start-websocket/{symbol}", encoders = OHLCVEncoder.class)
@ApplicationScoped
public class WebSocketServer {

	private static Map<String, ConcurrentLinkedDeque<Session>> sessions = new ConcurrentHashMap<>();

	@OnOpen
	public void onOpen(Session session, @PathParam("symbol") String symbol) {
		sessions.computeIfAbsent(symbol,  s -> new ConcurrentLinkedDeque<>());
		sessions.get(symbol).add(session);
		System.out.println("onOpen> " + symbol);
	}

	@OnClose
	public void onClose(Session session, @PathParam("symbol") String symbol) {
		sessions.get(symbol).remove(session);
		System.out.println("onClose> " + symbol);
	}

	@OnError
	public void onError(Session session, @PathParam("symbol") String symbol, Throwable throwable) {
		sessions.get(symbol).remove(session);
		System.out.println("onError> " + symbol + ": " + throwable);
	}

	@OnMessage
	public void onMessage(String message, @PathParam("symbol") String symbol) {
		System.out.println("onMessage> " + symbol + ": " + message);
	}

	@Incoming("symbol-in-memory")
	public void broadcastAsync(TopicMsg message) {
		sessions.keySet().parallelStream().forEach(symbolKey -> 
			sessions.get(symbolKey).parallelStream().forEach(session -> {
				if (symbolKey.equals(message.getSymbol())) {
					sendAsync(session, message.toOHLCV());
				}
			})
		);
	}

	private void sendAsync(Session s, OHLCV message) {
		s.getAsyncRemote().sendObject(message, result -> {
			if (result.getException() != null) {
				System.out.println("Unable to send message: " + result.getException());
			}
		});
	}
}
