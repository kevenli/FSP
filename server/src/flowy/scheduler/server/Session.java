package flowy.scheduler.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

import com.google.protobuf.GeneratedMessage.GeneratedExtension;

import flowy.scheduler.entities.Task;
import flowy.scheduler.entities.TaskInstance;
import flowy.scheduler.entities.TaskStatus;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.RegisterTask;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse.RegisterTaskResultType;
import flowy.scheduler.protocal.Messages.Response;
import flowy.scheduler.protocal.Messages.Response.ResponseType;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.server.data.TaskDAO;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class Session{
	private Scheduler m_scheduler;
	private int m_sessionId;
	private int applicationId;
	private Channel channel;
	private List<String> jobs = new ArrayList<String>();

	public Session(int sessionId, int applicationId, SessionHandler sessionHandler, Scheduler scheduler){
		m_sessionId = sessionId;
		this.applicationId = applicationId;
		this.m_scheduler = scheduler;
	}
	
	public int getId() {
		return this.m_sessionId;
	}
	
	public void setChannel(Channel channel){
		this.channel = channel;
	}

	public void onNotify(int taskId){
		TaskDAO dao = new TaskDAO();
		Task task = dao.getTask(taskId);
		
		TaskInstance instance = new TaskInstance();
		instance.setId(UUID.randomUUID().toString());
		instance.setTaskId(task.getId());
		instance.setSessionId(m_sessionId);
		instance.setFireTime(new Date());
		instance.setStatus(TaskStatus.NotStart);
		
		dao.saveTaskInstance(instance);
		
		TaskNotify notify = TaskNotify.newBuilder()
				.setTaskId(task.getClientTaskId())
				.setTaskInstanceId(instance.getId())
				.build();
		
		channel.writeAndFlush(buildResponseMessage(ResponseType.TASK_NOTIFY, 
				Messages.taskNotify, notify));
	}
	
	public void bindHandler(SessionHandler sessionHandler){
	}
	
	public void onRegisterTask(ChannelHandlerContext ctx, RegisterTask registerTaskRequest) throws SchedulerException {
		TaskDAO taskDAO = new TaskDAO();
		Task task = taskDAO.getTask(this.applicationId, registerTaskRequest.getTaskId());
		
		if(task!=null && !task.getExecuteTime().equals(registerTaskRequest.getExecuteTime())){
			// task already exists with different setting.
			RegisterTaskResponse responseMessage = RegisterTaskResponse.newBuilder()
					.setRegisterResult(RegisterTaskResultType.TASK_ALREADY_EXISTS)
					.build();
			ctx.writeAndFlush(buildResponseMessage(ResponseType.REGISTER_TASK_RESPONSE,
					Messages.registerTaskResponse, 
					responseMessage));
			return;
		}
		
		if(task == null){
			task = new Task();
		}
		task.setApplicationId(applicationId);
		task.setClientTaskId(registerTaskRequest.getTaskId());
		task.setExecuteTime(registerTaskRequest.getExecuteTime());
		taskDAO.saveTask(task);
		
		// register quartz scheduler
		String jobName = this.m_sessionId + "_" + registerTaskRequest.getTaskId() + "_job";
		JobDetail job = newJob(TaskNotifyJob.class).withIdentity(
				jobName, 
				"group1").build();
		
		job.getJobDataMap().put("SessionInstance", this);
		job.getJobDataMap().put("TaskId", task.getId());
		
    	Trigger trigger = newTrigger().withIdentity(
				this.m_sessionId + "_" + registerTaskRequest.getTaskId() + "_job", 
				"group1")
				.startNow().withSchedule(
						cronSchedule(registerTaskRequest.getExecuteTime())).build();
    	
		// Tell quartz to schedule the job using our trigger
		m_scheduler.scheduleJob(job, trigger);
		jobs.add(jobName);
		RegisterTaskResponse responseMessage = RegisterTaskResponse.newBuilder()
				.setRegisterResult(RegisterTaskResultType.SUCCESS)
				.build();
		ctx.writeAndFlush(buildResponseMessage(ResponseType.REGISTER_TASK_RESPONSE,
				Messages.registerTaskResponse, 
				responseMessage));
	}
	
	private static <Type> Response buildResponseMessage(ResponseType responseType, 
			final GeneratedExtension<Response, Type> extension,
	        final Type value){
		Response response = Response.newBuilder()
				.setType(responseType)
				.setExtension(extension, value).build();
		return response;
	}

	public void onTaskStatusUpdate(ChannelHandlerContext ctx,
			TaskStatusUpdate taskStatusUpdate) {
		TaskDAO dao = new TaskDAO();
		TaskInstance instance = dao.getTaskInstance(taskStatusUpdate.getInstanceId());
		if (taskStatusUpdate.getStatus() == Status.START){
			instance.setStatus(TaskStatus.Start);
			instance.setStartTime(new Date());
			instance.setUpdateTime(new Date());
			dao.updateTaskInstance(instance);
		}else if(taskStatusUpdate.getStatus() == Status.RUNNING){
			instance.setStatus(TaskStatus.Running);
			instance.setUpdateTime(new Date());
			dao.updateTaskInstance(instance);
		}else if(taskStatusUpdate.getStatus() == Status.COMPLETE){
			instance.setStatus(TaskStatus.Success);
			instance.setUpdateTime(new Date());
			instance.setCompleteTime(new Date());
			dao.updateTaskInstance(instance);
		}else if(taskStatusUpdate.getStatus() == Status.FAILED){
			instance.setStatus(TaskStatus.Failed);
			instance.setUpdateTime(new Date());
			instance.setCompleteTime(new Date());
			dao.updateTaskInstance(instance);
		}
		
	}

	public void teardown() {
		for(String jobName :jobs){
			try {
				this.m_scheduler.deleteJob(new JobKey(jobName, "group1"));
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
		}
		channel.close();
	}
}

