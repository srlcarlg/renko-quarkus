package creator.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import creator.core.services.RenkoChartService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@ClientEndpoint(decoders = TickDecoder.class)
@ApplicationScoped
public class WebSocketClient {

	private static Map<Session, String> sessions = new ConcurrentHashMap<>();
	@Inject
	private RenkoChartService chartService;

	@OnOpen
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		String symbol = getSymbolFromSession(session);
		sessions.put(session, symbol);
		session.getAsyncRemote().sendText("_ready_");

		System.out.println("CLIENT: onOpen> " + session.toString() + " on " + symbol);
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("onClose> " + session + " on " + getSymbolFromSession(session));
		sessions.remove(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.out.println("onError> " + session + " on " + getSymbolFromSession(session) + throwable);
		sessions.remove(session);
	}

	@OnMessage
	public void onMessage(Tick message, Session session) {
		String symbol = getSymbolFromSession(session);
		chartService.initRenko(symbol, message.getDatetime(), Double.valueOf(message.getBid()),
				symbol.equals("EURGBP") ? 0.0003 : 5D);
		chartService.addPrices(symbol, message.getDatetime(), Double.valueOf(message.getBid()));
		chartService.makeChart(symbol, "normal");
		// System.out.println("CLIENT: OnMessage> " + symbol ": " + message);
	}

	private static final String getSymbolFromSession(Session session) {
		String urlPath = session.getRequestURI().getPath();
		return urlPath.substring(urlPath.lastIndexOf('/') + 1);
	}
}
