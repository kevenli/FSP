package flowy.scheduler.server;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TaskNotifyJob implements Job {

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		Session session = (Session) arg0.getJobDetail().getJobDataMap()
					.get("SessionInstance");
		session.onNotify();
	}

	public TaskNotifyJob() {

	}
}

