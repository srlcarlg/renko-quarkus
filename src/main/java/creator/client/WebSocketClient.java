package creator.client;

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

	@Inject
	RenkoChartService renkoService;

	@OnOpen
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		String symbol = getSymbolFromSession(session);
		session.getAsyncRemote().sendText("_ready_");
		System.out.println("CLIENT: onOpen> " + session.toString() + " on " + symbol);
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("onClose> " + session + " on " + getSymbolFromSession(session));
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.out.println("onError> " + session + " on " + getSymbolFromSession(session) + throwable);
		System.out.println(throwable.getClass());
		closeSession(session);
	}

	@OnMessage
	public void onMessage(Tick message, Session session) {
		String symbol = getSymbolFromSession(session);
		renkoService.buildRenko(symbol, message.getDatetime(), Double.valueOf(message.getBid()),
				symbol.equals("EURGBP") ? 0.0003 : 5D);
	}

	private void closeSession(Session session) {
		try {
			session.close();
		} catch (Exception e) { }
	}

	private static final String getSymbolFromSession(Session session) {
		String urlPath = session.getRequestURI().getPath();
		return urlPath.substring(urlPath.lastIndexOf('/') + 1);
	}
}
