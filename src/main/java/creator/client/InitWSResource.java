package creator.client;

import java.net.URI;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/initws")
public class InitWSResource {

	private final URI uriEURGBP = URI.create("ws://localhost:8080/start-ws/EURGBP");
	private final URI uriUS30 = URI.create("ws://localhost:8080/start-ws/US30");
		
	@GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{symbol}")
	public String initWs(String symbol) {
		WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
		try {
			wsContainer.connectToServer(WebSocketClient.class, symbol.equals("EURGBP") ? uriEURGBP : uriUS30);
			return "Done";
		} catch (Exception e) {
			return "Error " + e.toString();
		}
	}
}
