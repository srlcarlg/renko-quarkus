package creator.client;

import creator.client.entities.SessionInfo;
import creator.client.entities.Tick;
import creator.client.entities.TickDecoder;
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
	@Inject
	SessionService sessionService;

	@OnOpen
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		System.out.println("CLIENT: onOpen> " + session.toString());
		session.getAsyncRemote().sendText("_ready_");
	}

	@OnClose
	public void onClose(Session session) {
		SessionInfo info = sessionService.getSessionInfo(session);
		System.out.println("CLIENT: onClose> " + session + " on " + info.getSymbol());
		sessionService.removeSession(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		SessionInfo info = sessionService.getSessionInfo(session);
		System.out.println("CLIENT: onError> " + session + " on " + info.getSymbol() + ": " + throwable);
		sessionService.removeSession(session);
		closeSession(session);
	}

	@OnMessage
	public void onMessage(Tick message, Session session) {
		SessionInfo info = sessionService.getSessionInfo(session);
		renkoService.buildRenko(
				info.getSymbol(),
				message.getDatetime(), Double.valueOf(message.getBid()),
				info.getBrickSize()
		);
	}

	private void closeSession(Session session) {
		try {
			session.close();
		} catch (Exception e) { }
	}
}
