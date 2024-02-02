package simulator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/ticks/{symbol}", encoders = TickEncoder.class)
@ApplicationScoped
public class StartWebSocket {

	private Map<String, Session> sessions = new ConcurrentHashMap<>();
	private Map<String, List<Tick>> ticks = new ConcurrentHashMap<>();

	@Inject
	SimulatorService service;

	@OnOpen
	public void onOpen(Session session, @PathParam("symbol") String symbol) {
		sessions.put(symbol, session);
		ticks.computeIfAbsent(symbol, s -> service.getTickList(symbol));
	}

	@OnClose
	public void onClose(Session session, @PathParam("symbol") String symbol) {
		sessions.remove(symbol, session);
		System.out.println("onClose> " + symbol);
	}

	@OnError
	public void onError(Session session, @PathParam("symbol") String symbol, Throwable throwable) {
		sessions.remove(symbol, session);
		System.out.println("onError> " + symbol + ": " + throwable);
	}

	@OnMessage
	public void onMessage(String message, @PathParam("symbol") String symbol) {
		System.out.println("onMessage> " + symbol + ": " + message);
		if (message.equals("_ready_")) {
			streamTicks(symbol);
		}
	}

	private void streamTicks(String symbol) {
		try {Thread.sleep(1000);} catch (Exception e) {}
		if (sessions != null && (sessions.size() != 0)) {
			System.out.println("in Loop " + symbol);
			for (int i = 0; i < ticks.get(symbol).size(); i++) {
				try {Thread.sleep(200);} catch (Exception e) {}
				broadcastAsync(symbol, ticks.get(symbol).get(i));
			}
			System.out.println("in Loop FINISHED " + symbol);
		}
	}

	private void broadcastAsync(String symbol, Tick message) {
		Session s = sessions.get(symbol);
		s.getAsyncRemote().sendObject(message, result -> {
			if (result.getException() != null) {
				System.out.println("Unable to send message: " + result.getException());
			}
		});
	}
}
