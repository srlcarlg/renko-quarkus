package creator.client;

import org.jboss.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(WebSocketClient.class);

	@Inject
	RenkoChartService renkoService;
	@Inject
	SessionService sessionService;

	@OnOpen
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		session.getAsyncRemote().sendText("_ready_");
	}

	@OnClose
	public void onClose(Session session) {
		SessionInfo info = sessionService.getSessionInfo(session);
		sessionService.removeSession(session);
		LOG.info(String.format("CLIENT: onClose> %s", info.getSymbol()));

	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		SessionInfo info = sessionService.getSessionInfo(session);
		sessionService.removeSession(session);
		closeSession(session);
		LOG.error(String.format("CLIENT: onError> on %s: %s", info.getSymbol(), throwable));
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
