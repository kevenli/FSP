package flowy.scheduler.server.data;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import flowy.scheduler.entities.SessionVO;


public class SessionDAO extends DAOBase {
	
	private static Logger logger = Logger.getLogger(SessionDAO.class);
	
	public SessionDAO() {

	}
	
	public SessionVO SaveSession(SessionVO sessionVO){
		synchronized(SessionDAO.class){
			Session session = openSession();
			try{
				Transaction trans = session.beginTransaction();
				session.saveOrUpdate(sessionVO);
				session.flush();
				trans.commit();
				return sessionVO;
			}
			finally{
				session.close();
			}
		}
		
	}
	
	public void deleteSession(int sessionId){
		synchronized(SessionDAO.class){
			Session session = openSession();
			try{
				Transaction trans = session.beginTransaction();
				SessionVO sessionVO = (SessionVO)session.get(SessionVO.class, new Integer(sessionId));
				if (sessionVO==null){
					logger.warn("deleteSession, cannot find session " + sessionId);
					return;
				}
				session.delete(sessionVO);
				session.flush();
				trans.commit();
			}
			finally{
				session.close();
			}
		}
	}

	public SessionVO getSession(int sessionId) {
		Session session = openSession();
		try{
			return (SessionVO)session.get(SessionVO.class, sessionId);
		}
		finally{
			session.close();
		}
	}

}
