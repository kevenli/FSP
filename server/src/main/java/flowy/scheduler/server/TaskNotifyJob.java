package flowy.scheduler.server;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TaskNotifyJob implements Job {

	private static Logger logger = Logger.getLogger(TaskNotifyJob.class);
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		JobDataMap dataMap = arg0.getJobDetail().getJobDataMap();
		int sessionId = (int)dataMap.get("SessionId");
		int taskId = (int)dataMap.get("TaskId");
		
		Session session = SessionManager.getInstance().findSession(sessionId);
		if (session==null){
			logger.warn("Session " + sessionId + " does not exist.");
			return;
		}
		session.onNotify(taskId);
	}

	public TaskNotifyJob() {

	}
}

