package flowy.scheduler.server;

import io.netty.channel.Channel;

import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import flowy.scheduler.entities.SessionVO;
import flowy.scheduler.server.data.SessionDAO;

public class SessionManager {

	private static Logger logger = Logger.getLogger(SessionManager.class);
	
	private Scheduler scheduler;
	private SessionManager() throws SchedulerException {
		scheduler = StdSchedulerFactory.getDefaultScheduler();
	}

	private static SessionManager instance;

	public static SessionManager getInstance(){
		if (instance == null) {
			synchronized (SessionManager.class) {
				if (instance == null) {
					try {
						instance = new SessionManager();
					} catch (SchedulerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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

	public Session newSession(int applicationId, String remoteAddress, Channel channel) {
		synchronized (this) {
			// generate random sessionId
			int newSessionId;
			do {
				newSessionId = m_randomSeed.nextInt();
			} while (newSessionId <= 0 || m_sessions.containsKey(newSessionId));
			
			Session session = new Session(newSessionId, applicationId, scheduler, channel);
			
			// save to database
			SessionDAO dao = new SessionDAO();
			SessionVO sessionVO = new SessionVO();
			sessionVO.setApplicationId(applicationId);
			sessionVO.setId(newSessionId);
			sessionVO.setClientIp(remoteAddress);
			sessionVO.setCreateTime(new Date());
			dao.SaveSession(sessionVO);
			
			// add to collection
			m_sessions.put(newSessionId, session);
			
			return session;
		}
	}

	public void sessionTimeout(Session session) {
		int sessionId = session.getId();
		logger.debug("Session timeout : " + sessionId);
		session.teardown();
		m_sessions.remove(sessionId);
	}
	
	public void sessionLogout(Session session){
		int sessionId = session.getId();
		logger.debug("Session logout : " + sessionId);
		session.onLogout();
		m_sessions.remove(sessionId);
	}

	public Session resumeSession(Session session, int sessionId, Channel channel) {
		if (!this.m_sessions.containsKey(sessionId)){
			return null;
		}
		
		Session resumeToSession = m_sessions.get(sessionId);
		session.suspend();
		session.teardown();
		m_sessions.remove(session.getId());
		resumeToSession.resume(channel);
		return resumeToSession;
	}
}
