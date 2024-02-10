package creator.client;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import creator.client.entities.SessionInfo;
import jakarta.inject.Inject;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/client")
public class StartClientResource {
    private static final Logger LOG = Logger.getLogger(StartClientResource.class);

	@ConfigProperty(name = "creator.client.simulator.url")
	private String urlSimulator;

	@Inject
	private SessionService service;

	@GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{symbol}/{brickSize}")
	public String initWs(String symbol, Double brickSize) {
		if (service.symbolExists(symbol)) {
			return String.format("Error: Symbol %s already exists.", symbol);
		}
		
		WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
		try {
			URI uri = URI.create(String.format("ws://%s/%s", urlSimulator, symbol));
			SessionInfo info = new SessionInfo(symbol, brickSize);
			Session session = wsContainer.connectToServer(WebSocketClient.class, uri);
			
			LOG.info(String.format("CLIENT: onOpen> %s", info.getSymbol()));
			service.addSession(session, info);
			return "Done";
		} catch (Exception e) {
			return String.format("Error %s %ncannot connect to simulator server, is it ON?", e.toString());
		}
	}
}
