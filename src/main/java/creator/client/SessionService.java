package creator.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import creator.client.entities.SessionInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

@ApplicationScoped
public class SessionService {
	
	private Map<Session, SessionInfo> sessionRenko = new ConcurrentHashMap<>(); 
	
	public void addSession(Session session, SessionInfo sessionInfo) {
		sessionRenko.put(session, sessionInfo);
	}
	
	public void removeSession(Session session) {
		sessionRenko.remove(session);
	}
	
	public SessionInfo getSessionInfo(Session session) {
		return sessionRenko.get(session);
	}
	
	public boolean symbolExists(String symbol) {
		return sessionRenko.values().parallelStream().anyMatch(x -> x.getSymbol().equals(symbol));
	}
}
