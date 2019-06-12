package flowy.scheduler.server;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import io.netty.channel.Channel;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import flowy.scheduler.entities.SessionVO;
import flowy.scheduler.server.data.SessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManager {

	private static Logger logger = LoggerFactory.getLogger(SessionManager.class);

	private static final String SUSPENSION_CHECK_GROUP = "sessionmanager.suspensioncheck";

	private Scheduler scheduler;

	private SessionManager() throws SchedulerException {
		scheduler = StdSchedulerFactory.getDefaultScheduler();
	}

	private static SessionManager instance;

	public static SessionManager getInstance() {
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

	public Session newSession(int applicationId, String remoteAddress,
			Channel channel) {
		synchronized (this) {
			// generate random sessionId
			int newSessionId;
			do {
				newSessionId = m_randomSeed.nextInt();
			} while (newSessionId <= 0 || m_sessions.containsKey(newSessionId));

			Session session = new Session(newSessionId, applicationId,
					scheduler, channel);

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
			logger.debug("New session established: " + newSessionId);
			return session;
		}
	}

	public void sessionTimeout(Session session) {
		int sessionId = session.getId();
		logger.debug("Session timeout : " + sessionId);
		session.suspend();
		scheduleSessionSuspendTimeout(session);
	}

	private void scheduleSessionSuspendTimeout(Session session) {
		// register quartz scheduler
		String jobName = session.getId() + "_suspension_timeout_" + session.getSuspensionId();
		JobDetail job = newJob(SessionClearJob.class).withIdentity(jobName,
				SUSPENSION_CHECK_GROUP).build();

		job.getJobDataMap().put("SessionId", session.getId());
		job.getJobDataMap().put("SuspensionId", session.getSuspensionId());

		Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
		calendar.add(Calendar.SECOND, 30);
		
		Trigger trigger = newTrigger()
				.withIdentity(jobName, SUSPENSION_CHECK_GROUP)
				.startAt(calendar.getTime())
				.build();

		// Tell quartz to schedule the job using our trigger
		try {
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}

	}

	public void sessionLogout(Session session) {
		int sessionId = session.getId();
		logger.debug("Session logout : " + sessionId);
		
		SessionDAO dao = new SessionDAO();
		dao.deleteSession(sessionId);
		session.onLogout();
		m_sessions.remove(sessionId);
		
	}

	public Session resumeSession(Session currentSession, int resumeToSessionId,
			Channel channel) {
		if (!this.m_sessions.containsKey(resumeToSessionId)) {
			currentSession.resumeFailed();
			return currentSession;
		}

		Session resumeToSession = m_sessions.get(resumeToSessionId);
		currentSession.unbindChannel();
		currentSession.teardown();
		m_sessions.remove(currentSession.getId());
		SessionDAO dao = new SessionDAO();
		dao.deleteSession(currentSession.getId());
		resumeToSession.resume(channel);
		return resumeToSession;
	}

	public void onSessionSuspensionTimeout(int sessionId, int suspensionId) {
		logger.debug("onSessionSuspensionTimeout");
		Session session = this.findSession(sessionId);
		if (session == null) {
			logger.warn("Cannot find session " + sessionId);
			return;
		}

		// since the session may be resumed or suspended multi times during the
		// period,
		// check the suspension id first.
		if (session.getSuspensionId() != suspensionId) {
			return;
		}

		session.teardown();
		m_sessions.remove(sessionId);

		SessionDAO dao = new SessionDAO();
		dao.deleteSession(sessionId);
	}

}
