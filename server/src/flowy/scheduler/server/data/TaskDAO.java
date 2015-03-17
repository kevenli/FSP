package flowy.scheduler.server.data;


import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import flowy.scheduler.entities.Application;
import flowy.scheduler.entities.Task;

public class TaskDAO {

	private SessionFactory m_session_factory;
	
	private Session OpenSession(){
		if (m_session_factory == null){
			m_session_factory = new Configuration().configure()
					.buildSessionFactory();
		}
		
		Session session = m_session_factory.openSession();
		return session;
	}
	public Task createTask(Task task){
		Session session = OpenSession();
		Transaction trans = session.beginTransaction();
		try{
			session.save(task);
			trans.commit();
			return task;
		}
		finally{
			session.close();
		}
		
		
	}
	
	public Task getTask(long id){
		Session session = OpenSession();
		Task task = (Task)session.get(Task.class, id);
		
		session.close();
		return task;
	}
	
	public Task getTask(int applicationId, String clientTaskId){
		Session session = OpenSession();
		Criteria criteria = session.createCriteria(Task.class);
		criteria.add(Restrictions.eq("applicationId", applicationId));
		criteria.add(Restrictions.eq("clientTaskId", clientTaskId));
		Task task = (Task)criteria.uniqueResult();
		return task;
	}
	
	public Task updateTask(Task task) {
		Session session = OpenSession();
		Transaction trans = session.beginTransaction();
		session.update(task);
		trans.commit();
		session.close();
		return task;
	}
	public Task saveTask(Task task) {
		Session session = OpenSession();
		Transaction trans = session.beginTransaction();
		session.saveOrUpdate(task);
		trans.commit();
		session.close();
		return task;
	}
}
