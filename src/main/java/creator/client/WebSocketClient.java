package creator.client;

import java.net.URI;

import creator.core.services.RenkoChartService;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

@ClientEndpoint(decoders = TickDecoder.class)
@ApplicationScoped
public class WebSocketClient {

	private final URI uri = URI.create("ws://localhost:8080/start-ws/EURGBP");
	private final WebSocketContainer wsContainer = ContainerProvider.getWebSocketContainer();
	
	@Inject
	private RenkoChartService service;
	
	private boolean first = false;
	private Session session;

	@Startup
	@Scheduled(every = "10s")
	void init() {
		try {
			if (session == null) {
				session = wsContainer.connectToServer(this, uri);
			}
		} catch (Exception e) {
			System.out.println("ERROR CLIENT: " + e);
		}
	}

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("CLIENT: onOpen> " + session.toString());
        
        this.session = session;
        session.getAsyncRemote().sendText("_ready_");
    }

    @OnMessage
    public void onMessage(Tick message) {
    	if (!first) {
    		service.initRenko(message.getDatetime(), Double.valueOf(message.getBid()), 0.0003);
    		first = true;
    	}
    	service.addPrices(message.getDatetime(), Double.valueOf(message.getBid()));
    	service.makeChart("normal");
        
    	// System.out.println("CLIENT: OnMessage> " + message);
    }

}
