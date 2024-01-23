package creator.client;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
			final URI uri = URI.create("ws://" + urlSimulator + "/" + symbol);
			SessionInfo info = new SessionInfo(symbol, brickSize);
			Session session = wsContainer.connectToServer(WebSocketClient.class, uri);
			service.addSession(session, info);
			return "Done";
		} catch (Exception e) {
			return "Error " + e.toString();
		}
	}
}
