package flowy.scheduler.server;

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

import flowy.scheduler.entities.Application;
import flowy.scheduler.entities.Task;
import flowy.scheduler.entities.TaskStatus;
import flowy.scheduler.entities.Worker;
import flowy.scheduler.entities.WorkerStatus;
import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.LoginResponse.LoginResultType;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest;
import flowy.scheduler.protocal.Messages.WorkerRegisterResponse;
import flowy.scheduler.server.messages.Message;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class Session implements Runnable {
	private Socket m_socket;
	private WorkerRegisterRequest m_request;
	private Scheduler m_scheduler;
	private Application m_application;
	private Worker m_worker;
	private Object m_iolock = new Object();
	private Object m_notifylock = new Object();
	private int m_sessionId;
	private SessionHandler m_sessionHandler;

	private static Logger logger = Logger.getLogger(Session.class);
	
	private InetAddress clientAddress;
	
	public Session(Scheduler scheduler, Socket socket) {
		this.m_scheduler = scheduler;
		this.m_socket = socket;
		this.clientAddress = socket.getInetAddress();
	}
	
	public Session(int sessionId, SessionHandler sessionHandler){
		m_sessionId = sessionId;
		m_sessionHandler = sessionHandler;
	}

	@Override
	public void run() {
		if (!Auth()) {
			try {
				m_socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		try {
			WaitRegister();

			while (true) {
				synchronized(m_notifylock){
					m_notifylock.wait();
				}
				TaskDAO dao = new TaskDAO();
				Task newTask = new Task();
				newTask.setWorkerId(m_worker.getId());
				newTask.setCreateTime(new Date());
				newTask.setUpdateTime(new Date());
				newTask.setStatus(TaskStatus.NotStart);

				dao.createTask(newTask);
				
				Notify(m_worker.getClientWorkerId(),
					Long.toString(newTask.getId()));
				
			}

		} 
		catch(SocketException e){
			logger.info(String.format("Connection closed %s", clientAddress));
			System.out.println(String.format("Connection closed %s", clientAddress));
			if ( m_worker != null){
				m_worker.setUpdateTime(new Date());
				m_worker.setStatus(WorkerStatus.Offline);
				WorkerDAO dao = new WorkerDAO();
				dao.updateWorker(m_worker);
			}
		}
		catch (IOException e) {
			logger.error(e);
		} 
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}
	}

	private boolean Auth() {
		try {
			LoginRequest loginRequest = LoginRequest
					.parseFrom(getNextMessage());
			String app_id = loginRequest.getAppKey();
			String app_secret = loginRequest.getAppSecret();

			Application application = ApplicationDAO.getApplication(app_id);
			if (application != null
					&& application.getAppSecret().equals(app_secret)) {
				// authorization successed
				m_application = application;
				LoginResponse.Builder builder = LoginResponse.newBuilder();
				builder.setResultType(LoginResultType.SUCCESS);
				LoginResponse response = builder.build();
				send(response.toByteArray());
				return true;
			}

			LoginResponse.Builder builder = LoginResponse.newBuilder();
			builder.setResultType(LoginResultType.FAILED);
			LoginResponse response = builder.build();
			send(response.toByteArray());

			return false;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
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

		m_worker = dao.createOrUpdate(newWorker);

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

	private void Notify(String workerId, String taskId)
			throws InvalidProtocolBufferException, IOException {
		TaskNotify.Builder builder = TaskNotify.newBuilder();
		builder.setWorkerId(workerId);
		builder.setTaskId(taskId);

		send(builder.build().toByteArray());

		TaskDAO dao = new TaskDAO();
		
		TaskStatusUpdate status = null;
				
		do{
			status = TaskStatusUpdate.parseFrom(getNextMessage());
			
			
			long task_id = Long.parseLong(status.getTaskId());
			Task task = dao.getTask(task_id);
			task.setUpdateTime(new Date());
			TaskStatus task_status = TaskStatus.Unknown;
			switch (status.getStatus().getNumber()){
			case Status.START_VALUE:
				task_status = TaskStatus.Start;
				task.setStartTime(new Date());
				break;
			case Status.RUNNING_VALUE:
				task_status = TaskStatus.Running;
				break;
			case Status.STOP_VALUE:
				task_status = TaskStatus.Success;
				task.setCompleteTime(new Date());
				break;
			default:
				task_status = TaskStatus.Unknown;
			}
			
			task.setStatus( task_status );
			task.setUpdateTime(new Date());
			dao.updateTask(task);
		
		}
		while (status.getStatus() != Status.STOP);
	}
	
	public void onNotify(){
		synchronized(m_notifylock){
			m_notifylock.notify();
		}
	}
	
	public void handleMessage(Message message){
		
	}
}

