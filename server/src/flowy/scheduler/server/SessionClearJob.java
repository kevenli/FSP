package flowy.scheduler.server;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SessionClearJob implements Job {
	@Override
	public void execute(JobExecutionContext ctx)
			throws JobExecutionException {
		JobDataMap dataMap = ctx.getJobDetail().getJobDataMap();
		int sessionId = dataMap.getInt("SessionId");
		int suspensionId = dataMap.getInt("SuspensionId");
		SessionManager.getInstance().onSessionSuspensionTimeout(sessionId,
				suspensionId);
	}
	
	public SessionClearJob(){
		
	}
}