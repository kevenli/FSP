package flowy.scheduler.server;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskNotifyJob implements Job {

	private static Logger logger = LoggerFactory.getLogger(TaskNotifyJob.class);
	
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		JobDataMap dataMap = arg0.getJobDetail().getJobDataMap();
		int sessionId = dataMap.getInt("SessionId");
		int taskId = dataMap.getInt("TaskId");
		
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

