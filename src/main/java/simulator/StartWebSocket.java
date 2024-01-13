package simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/start-ws/{name}", encoders = TickEncoder.class)
@ApplicationScoped
public class StartWebSocket {

    Map<String, Session> sessions = new ConcurrentHashMap<>();
	List<Tick> ticks = new ArrayList<>();
	SimulatorService service;
	boolean already = false;

    @OnOpen
    public void onOpen(Session session, @PathParam("name") String name) {
    	sessions.put(name, session);
    	
    	service = new SimulatorService();
    	ticks = service.getTickList(name);

    	// System.out.println("onOpen> " + ticks.toString());
    }

    @OnClose
    public void onClose(Session session, @PathParam("name") String name) {
    	sessions.remove(name, session);
        System.out.println("onClose> " + name);
    }

    @OnError
    public void onError(Session session, @PathParam("name") String name, Throwable throwable) {
    	sessions.remove(name, session);
        System.out.println("onError> " + name + ": " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("name") String name) {
        System.out.println("onMessage> " + name + ": " + message);
    }

    @Scheduled(every="10s")
    void increment() {
        if (!already && sessions != null && (sessions.size() != 0)) {
        	System.out.println("in Loop");
        	for (int i = 0; i < ticks.size(); i++) {
        		//try { Thread.sleep(500); } catch (Exception e) {}
                already = true;
                broadcastAsync(ticks.get(i));
			}
        	System.out.println("in Loop FINISHED");
        }
    }

    private void broadcastAsync(Tick message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
