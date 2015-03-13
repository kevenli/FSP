package flowy.scheduler.server;

import java.util.Hashtable;
import java.util.Random;

public class SessionManager {

	public SessionManager() {

	}

	private static SessionManager instance;

	public static SessionManager getInstance() {
		if (instance == null) {
			synchronized (SessionManager.class) {
				if (instance == null) {
					instance = new SessionManager();
				}
			}
		}
		return instance;
	}

	private Hashtable<Integer, Session> m_sessions = new Hashtable<Integer, Session>();

	private Random m_randomSeed = new Random();

	public Session findSession(int sessionId) {
		if (m_sessions.containsKey(sessionId)) {
			return m_sessions.get(sessionId);
		}
		return null;
	}

	public Session newSession(SessionHandler handler) {
		synchronized (this) {
			int newSessionId;
			do {
				newSessionId = m_randomSeed.nextInt();
			} while (newSessionId <= 0 || m_sessions.containsKey(newSessionId));
			Session session = new Session(newSessionId, handler);
			m_sessions.put(newSessionId, session);
			return session;
		}
	}
}
