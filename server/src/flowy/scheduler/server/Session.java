package flowy.scheduler.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

import com.google.protobuf.GeneratedMessage.GeneratedExtension;

import flowy.scheduler.entities.SessionVO;
import flowy.scheduler.entities.Task;
import flowy.scheduler.entities.TaskInstance;
import flowy.scheduler.entities.TaskStatus;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.LogoutResponse;
import flowy.scheduler.protocal.Messages.RegisterTask;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse;
import flowy.scheduler.protocal.Messages.ResumeSessionResponse;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse.RegisterTaskResultType;
import flowy.scheduler.protocal.Messages.Response;
import flowy.scheduler.protocal.Messages.Response.ResponseType;
import flowy.scheduler.protocal.Messages.ResumeSessionResponse.ResumeResultType;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.protocal.Messages.UnregisterTask;
import flowy.scheduler.protocal.Messages.UnregisterTaskResponse;
import flowy.scheduler.server.data.SessionDAO;
import flowy.scheduler.server.data.TaskDAO;
import flowy.scheduler.server.util.RandomUtil;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class Session{
	
	private static final String DEFAULT_GROUP_NAME = "session_tasks";
	
	private static Logger logger = Logger.getLogger(Session.class);
	
	private Scheduler m_scheduler;
	private int m_sessionId;
	private int applicationId;
	private Channel channel;
	private List<String> jobs = new ArrayList<String>();
	private int suspensionId = -1;
	
	private Hashtable<Integer, Queue<TaskInstance>> taskQueues = new Hashtable<Integer, Queue<TaskInstance>>();
	
	private TaskDAO taskDAO = new TaskDAO();
	
	private SessionDAO sessionDAO = new SessionDAO();

	public Session(int sessionId, int applicationId, Scheduler scheduler, Channel channel){
		m_sessionId = sessionId;
		this.applicationId = applicationId;
		this.m_scheduler = scheduler;
		this.channel = channel;
	}
	
	public int getId() {
		return this.m_sessionId;
	}
	
	public void resume(Channel channel){
		// resume the session with channel
		this.channel = channel;
		
		this.suspensionId = -1;
		
		
		ResumeSessionResponse resumeSessionResponse = ResumeSessionResponse.newBuilder()
				.setResultType(ResumeResultType.SUCCESS)
				.setSessionId(this.m_sessionId)
				.build();
		
		channel.writeAndFlush(buildResponseMessage(Response.ResponseType.RESUME_SESSION_RESPONSE,
				Messages.resumeSessionResponse, resumeSessionResponse));
		
		// send all suspended notifications
		Enumeration<Integer> keys=taskQueues.keys();
		while(keys.hasMoreElements()){
			int taskId = keys.nextElement();
			Queue<TaskInstance> queue = taskQueues.get(taskId);
			TaskInstance taskInstance = queue.peek();
			if (taskInstance != null){
				sendTaskNotification(taskInstance);
			}
		}
	}

	public void onNotify(int taskId){
		logger.debug("On notify, taskId:" + taskId);
		
		Queue<TaskInstance> queue = null;
		synchronized(taskQueues){
			if(!taskQueues.containsKey(taskId)){
				queue = new ConcurrentLinkedDeque<TaskInstance>();
				taskQueues.put(taskId, queue);
			}else{
				queue = taskQueues.get(taskId);
			}
		}
		
		Task task = taskDAO.getTask(taskId);
		// at most contains 2 elements: 1 executing, 1 waiting
		// otherwise, ignore the new fire.

		int queueSize = queue.size();
		if (queueSize<2){
			TaskInstance instance = new TaskInstance();
			instance.setId(UUID.randomUUID().toString());
			instance.setTaskId(task.getId());
			instance.setSessionId(m_sessionId);
			instance.setFireTime(new Date());
			instance.setStatus(TaskStatus.NotStart);
			
			taskDAO.saveTaskInstance(instance);
			queue.offer(instance);

			// if this is the first element in the queue, send it to client.
			if (queueSize == 0){
				sendTaskNotification(instance);
			}
		}
	}
	
	public void bindHandler(SessionHandler sessionHandler){
	}
	
	public void onRegisterTask(ChannelHandlerContext ctx, RegisterTask registerTaskRequest) throws SchedulerException {
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
		task = taskDAO.saveTask(task);
		
		SessionVO vo = sessionDAO.getSession(this.m_sessionId);
		vo.getTasks().add(task);
		sessionDAO.SaveSession(vo);
		
		// register quartz scheduler
		String jobName = this.m_sessionId + "_" + registerTaskRequest.getTaskId() + "_job";
		JobDetail job = newJob(TaskNotifyJob.class).withIdentity(
				jobName, 
				DEFAULT_GROUP_NAME).build();
		
		job.getJobDataMap().put("SessionId", this.m_sessionId);
		job.getJobDataMap().put("TaskId", task.getId());
		
    	Trigger trigger = newTrigger().withIdentity(
				this.m_sessionId + "_" + registerTaskRequest.getTaskId() + "_job", 
				DEFAULT_GROUP_NAME)
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
		TaskInstance instance = taskDAO.getTaskInstance(taskStatusUpdate.getInstanceId());
		if (instance == null){
			logger.warn("Cannot find task instance " + taskStatusUpdate.getInstanceId());
			return;
		}
		int taskId = instance.getTaskId();
		
		if (taskStatusUpdate.getStatus() == Status.START){
			instance.setStatus(TaskStatus.Start);
			instance.setStartTime(new Date());
			instance.setUpdateTime(new Date());
			taskDAO.updateTaskInstance(instance);
		}else if(taskStatusUpdate.getStatus() == Status.RUNNING){
			instance.setStatus(TaskStatus.Running);
			instance.setUpdateTime(new Date());
			taskDAO.updateTaskInstance(instance);
		}else if(taskStatusUpdate.getStatus() == Status.COMPLETE){
			instance.setStatus(TaskStatus.Success);
			instance.setUpdateTime(new Date());
			instance.setCompleteTime(new Date());
			taskDAO.updateTaskInstance(instance);
			pollAndNotifyNext(taskId, instance.getId());
		}else if(taskStatusUpdate.getStatus() == Status.FAILED){
			instance.setStatus(TaskStatus.Failed);
			instance.setUpdateTime(new Date());
			instance.setCompleteTime(new Date());
			taskDAO.updateTaskInstance(instance);
			pollAndNotifyNext(taskId, instance.getId());
		}
	}
	
	private void pollAndNotifyNext(int taskId, String instanceId){
		Queue<TaskInstance> queue = taskQueues.get(taskId);
		if (queue == null){
			return;
		}
		// remove the first element which is complete
		// when session resumed, the same task instance may be notified more than once, check the instance id to poll.
		TaskInstance taskInstanceToPoll = queue.peek();
		if (taskInstanceToPoll!= null && taskInstanceToPoll.getId().equals(instanceId)){
			// remove it
			queue.poll();
		}
		
		// if next task instance exists
		TaskInstance instance = queue.peek();
		if (instance != null){
			Task task = taskDAO.getTask(taskId);
			TaskNotify notify = TaskNotify.newBuilder()
					.setTaskId(task.getClientTaskId())
					.setTaskInstanceId(instance.getId())
					.build();
			
			channel.writeAndFlush(buildResponseMessage(ResponseType.TASK_NOTIFY, 
					Messages.taskNotify, notify));
		}
	}

	public void teardown() {
		for(String jobName :jobs){
			try {
				this.m_scheduler.deleteJob(new JobKey(jobName, DEFAULT_GROUP_NAME));
			} catch (SchedulerException e) {
				e.printStackTrace();
			}
		}
		
		if (channel != null){
			channel.close();
		}
	}

	public void onLogout() {
		LogoutResponse logoutResponse = LogoutResponse.newBuilder().build();
		
		channel.writeAndFlush(buildResponseMessage(Response.ResponseType.LOGOUT_RESPONSE,
				Messages.logoutResponse, logoutResponse)).syncUninterruptibly();
		this.teardown();
	}

	public void suspend() {
		this.suspensionId = RandomUtil.randomPositiveInt();
		this.channel = null;
	}
	
	public void unbindChannel(){
		this.channel = null;
	}
	
	public int getSuspensionId(){
		return this.suspensionId;
	}
	
	private void sendTaskNotification(TaskInstance taskInstance){
		Task task = taskDAO.getTask(taskInstance.getTaskId());
		TaskNotify notify = TaskNotify.newBuilder()
				.setTaskId(task.getClientTaskId())
				.setTaskInstanceId(taskInstance.getId())
				.build();
		
		// channel would be null when suspending
		// notification will be sent when session resumed
		if (channel != null){
			channel.writeAndFlush(buildResponseMessage(ResponseType.TASK_NOTIFY, 
					Messages.taskNotify, notify));
		}
	}

	public void resumeFailed() {
		ResumeSessionResponse resumeSessionResponse = ResumeSessionResponse.newBuilder()
				.setResultType(ResumeResultType.SESSION_EXPIRED)
				.setSessionId(this.m_sessionId)
				.build();
		
		channel.writeAndFlush(buildResponseMessage(Response.ResponseType.RESUME_SESSION_RESPONSE,
				Messages.resumeSessionResponse, resumeSessionResponse));
	}

	public void onUnregisterTask(UnregisterTask request) {
		String taskClientId = request.getTaskId();
		Task task = taskDAO.getTask(this.applicationId, taskClientId);

		taskDAO.saveTask(task);
		
		// register quartz scheduler
		String jobName = this.m_sessionId + "_" + taskClientId + "_job";
		
    	
		// remove scheduled job
		try {
			m_scheduler.deleteJob(new JobKey(jobName, DEFAULT_GROUP_NAME));
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// remove from queue
		if (taskQueues.containsKey(task.getId())){
			taskQueues.remove(task.getId());
		}
		
		UnregisterTaskResponse responseMessage = UnregisterTaskResponse.newBuilder()
				.build();
		channel.writeAndFlush(buildResponseMessage(ResponseType.UNREGISTER_TASK_RESPONSE,
				Messages.unregisterTaskResponse, 
				responseMessage));
	}
	
}

