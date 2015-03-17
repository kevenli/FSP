package flowy.scheduler.server.data;

import org.hibernate.Session;
import org.hibernate.Transaction;
import flowy.scheduler.entities.SessionVO;

public class SessionDAO extends DAOBase {
	public SessionDAO() {

	}
	
	public SessionVO SaveSession(SessionVO sessionVO){
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		session.save(sessionVO);
		trans.commit();
		session.close();
		return sessionVO;
	}

}
