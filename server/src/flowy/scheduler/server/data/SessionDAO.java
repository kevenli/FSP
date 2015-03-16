package flowy.scheduler.server.data;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import flowy.scheduler.entities.SessionVO;

public class SessionDAO {
	private SessionFactory m_session_factory;
	
	private Session OpenSession(){
		if (m_session_factory == null){
			m_session_factory = new Configuration().configure()
					.buildSessionFactory();
		}
		
		Session session = m_session_factory.openSession();
		return session;
	}
	public SessionDAO() {

	}
	
	public SessionVO SaveSession(SessionVO sessionVO){
		Session session = OpenSession();
		Transaction trans = session.beginTransaction();
		session.save(sessionVO);
		trans.commit();
		session.close();
		return sessionVO;
	}

}
