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
	
	public void deleteSession(int sessionId){
		Session session = openSession();
		try{
			Transaction trans = session.beginTransaction();
			SessionVO sessionVO = (SessionVO)session.get(SessionVO.class, sessionId);
			if (sessionVO!=null){
				session.delete(sessionVO);
			}
			trans.commit();
		}
		finally{
			session.close();
		}
	}

}
