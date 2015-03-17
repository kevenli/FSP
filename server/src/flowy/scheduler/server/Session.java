package flowy.scheduler.server;

import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Date;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;

import flowy.scheduler.entities.Application;
import flowy.scheduler.entities.Task;
import flowy.scheduler.entities.TaskStatus;
import flowy.scheduler.entities.Worker;
import flowy.scheduler.entities.WorkerStatus;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.LoginResponse.LoginResultType;
import flowy.scheduler.protocal.Messages.RegisterTask;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse.RegisterTaskResultType;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Response;
import flowy.scheduler.protocal.Messages.Response.ResponseType;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest;
import flowy.scheduler.protocal.Messages.WorkerRegisterResponse;
import flowy.scheduler.server.data.ApplicationDAO;
import flowy.scheduler.server.data.TaskDAO;
import flowy.scheduler.server.data.WorkerDAO;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class Session{
	private Socket m_socket;
	private WorkerRegisterRequest m_request;
	private Scheduler m_scheduler;
	private Application m_application;
	private Object m_iolock = new Object();
	private Object m_notifylock = new Object();
	private int m_sessionId;
	private SessionHandler m_sessionHandler;
	private int applicationId;

	private static Logger logger = Logger.getLogger(Session.class);
	
	private InetAddress clientAddress;
	
	public Session(Scheduler scheduler, Socket socket) {
		this.m_scheduler = scheduler;
		this.m_socket = socket;
		this.clientAddress = socket.getInetAddress();
	}
	
	public Session(int sessionId, int applicationId, SessionHandler sessionHandler, Scheduler scheduler){
		m_sessionId = sessionId;
		this.applicationId = applicationId;
		m_sessionHandler = sessionHandler;
		this.m_scheduler = scheduler;
	}


	private void WaitRegister() throws IOException, SchedulerException {
		// receive register request
		WorkerRegisterRequest request = WorkerRegisterRequest
				.parseFrom(getNextMessage());
		this.m_request = request;

		WorkerDAO dao = new WorkerDAO();
		Worker newWorker = new Worker();
		newWorker.setApplicationId(m_application.getId());
		newWorker.setClientWorkerId(request.getWorkerId());
		newWorker.setName(request.getWorkerName());
		newWorker.setSchedule(request.getExecuteTime(0));
		newWorker.setTimeout(request.getTimeout());
		newWorker.setStatus(WorkerStatus.Online);
		newWorker.setCreateTime(new Date());
		newWorker.setUpdateTime(new Date());

		dao.createOrUpdate(newWorker);

		logger.debug(request);

		String ramdomString = StringUtil.getRandomString(10);

		// register quartz scheduler
		JobDetail job = newJob(TaskNotifyJob.class).withIdentity(
				newWorker.getClientWorkerId() + "_Job_" + ramdomString, "group1").build();
		job.getJobDataMap().put("SessionInstance", this);
		
		
		// Trigger the job to run now, and then every 40 seconds
		Trigger trigger = newTrigger().withIdentity(
				newWorker.getClientWorkerId() + "_trigger_" + ramdomString, "group1")
				.startNow().withSchedule(
				// simpleSchedule().withIntervalInSeconds(10).repeatForever()
						cronSchedule(this.m_request.getExecuteTime(0))).build();

		// Tell quartz to schedule the job using our trigger
		m_scheduler.scheduleJob(job, trigger);

		// send response back
		WorkerRegisterResponse.Builder builder = WorkerRegisterResponse
				.newBuilder();
		builder.setWorkerId(request.getWorkerId());
		WorkerRegisterResponse response = builder.build();
		send(response.toByteArray());
	}

	private byte[] getNextMessage() throws IOException {
		synchronized(m_iolock){
			byte[] sizeBuffer = new byte[4];
			m_socket.getInputStream().read(sizeBuffer, 0, 4);
			int size = ByteBuffer.wrap(sizeBuffer).getInt();
			byte[] buffer = new byte[size];
			m_socket.getInputStream().read(buffer, 0, size);
			return buffer;
		}
	}

	private void send(byte[] bytes) throws IOException {
		synchronized(m_iolock){
			int size = bytes.length;
			byte[] sizeBuffer = ByteBuffer.allocate(4).putInt(size).array();
			m_socket.getOutputStream().write(sizeBuffer);
			m_socket.getOutputStream().write(bytes);
		}
	}

	public void onNotify(){
		synchronized(m_notifylock){
			m_notifylock.notify();
		}
	}
	
	public void bindHandler(SessionHandler sessionHandler){
		this.m_sessionHandler = sessionHandler;
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
		JobDetail job = newJob(TaskNotifyJob.class).withIdentity(
				this.m_sessionId + "_" + registerTaskRequest.getTaskId() + "_job", 
				"group1").build();
		
		job.getJobDataMap().put("SessionInstance", this);
		
		
    	Trigger trigger = newTrigger().withIdentity(
				this.m_sessionId + "_" + registerTaskRequest.getTaskId() + "_job", 
				"group1")
				.startNow().withSchedule(
						cronSchedule(registerTaskRequest.getExecuteTime())).build();

		// Tell quartz to schedule the job using our trigger
		m_scheduler.scheduleJob(job, trigger);
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
}

