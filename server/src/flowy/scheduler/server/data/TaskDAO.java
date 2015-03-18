package flowy.scheduler.server.data;


import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import flowy.scheduler.entities.Task;
import flowy.scheduler.entities.TaskInstance;

public class TaskDAO extends DAOBase {

	public Task createTask(Task task){
		Session session = openSession();
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
	
	public Task getTask(int id){
		Session session = openSession();
		Task task = (Task)session.get(Task.class, id);
		
		session.close();
		return task;
	}
	
	public Task getTask(int applicationId, String clientTaskId){
		Session session = openSession();
		try{
			Criteria criteria = session.createCriteria(Task.class);
			criteria.add(Restrictions.eq("applicationId", applicationId));
			criteria.add(Restrictions.eq("clientTaskId", clientTaskId));
			Task task = (Task)criteria.uniqueResult();
			return task;
		}
		finally{
			session.close();
		}
	}
	
	public Task updateTask(Task task) {
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		session.update(task);
		trans.commit();
		session.close();
		return task;
	}
	public Task saveTask(Task task) {
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		session.saveOrUpdate(task);
		trans.commit();
		session.close();
		return task;
	}
	public TaskInstance saveTaskInstance(TaskInstance instance) {
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		session.save(instance);
		trans.commit();
		session.close();
		return instance;
	}
	
	public TaskInstance getTaskInstance(String id){
		Session session = openSession();
		try{
			return (TaskInstance)session.get(TaskInstance.class, id);
		}
		finally{
			session.close();
		}
	}
	
	public TaskInstance updateTaskInstance(TaskInstance taskInstance){
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		session.update(taskInstance);
		trans.commit();
		session.close();
		return taskInstance;
	}
}
