package flowy.scheduler.server;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TaskNotifyJob implements Job {

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		JobDataMap dataMap = arg0.getJobDetail().getJobDataMap();
		Session session = (Session) dataMap.get("SessionInstance");
		int taskId = (int)dataMap.get("TaskId");
		session.onNotify(taskId);
	}

	public TaskNotifyJob() {

	}
}

