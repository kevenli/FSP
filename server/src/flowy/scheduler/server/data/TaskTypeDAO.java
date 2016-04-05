package flowy.scheduler.server.data;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import flowy.scheduler.entities.TaskInstance;
import flowy.scheduler.entities.TaskType;

public class TaskTypeDAO extends DAOBase {

	public List<TaskType> GetAllTaskTypes(){
		Session session = openSession();
		try{
			return (List<TaskType>)session.createCriteria(TaskType.class).list();
		}
		finally{
			session.close();
		}
	}
	
	public TaskType SaveTaskType(TaskType taskType){
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		try{
			session.save(taskType);
			session.flush();
			trans.commit();
			return taskType;
		}
		finally{
			session.close();
		}
	}
}
