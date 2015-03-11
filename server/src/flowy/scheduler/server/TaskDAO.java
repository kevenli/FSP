package flowy.scheduler.server;


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

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
	public Task updateTask(Task task) {
		Session session = OpenSession();
		Transaction trans = session.beginTransaction();
		session.update(task);
		trans.commit();
		session.close();
		return task;
	}
}
