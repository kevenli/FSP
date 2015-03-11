package flowy.scheduler.javasdk;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest;
import flowy.scheduler.protocal.Messages.WorkerRegisterResponse;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest.ExecuteLastExpiredType;

public class Client {
	public static int DEFAULT_HOST_PORT = 3333;
	
	private String[] m_hosts;
	
	private Socket m_socket;
	
	private IClientCallback m_callback;
	
	private WorkerSetting m_worker_setting;
	
	private String m_app_key;
	
	private String m_app_secret;
	
	private Object m_iolock = new Object();
	
	public Client(String hosts, String app_key, String app_secret, WorkerSetting workerSetting, IClientCallback callback){
		
		m_hosts = hosts.split(";");
		m_callback = callback;
		m_worker_setting = workerSetting;
		m_app_key = app_key;
		m_app_secret = app_secret;
	}
	
	public void start() throws UnknownHostException, IOException, InterruptedException{
		String host = m_hosts[0];
		
		String[] host_parts = host.split(":");
		String host_name = host_parts[0];
		int host_port = DEFAULT_HOST_PORT;
		if (host_parts.length>0){
			host_port = Integer.parseInt(host_parts[1]);  // TO-DO:int.parse(host_parts[1]);
		}
		
		m_socket = new Socket(InetAddress.getByName(host_name), host_port);
		
		sendVersion();
		auth();
		register();
		while (true) {
			startListen();
		}
	}
	
	private void sendVersion() throws IOException {
		String version = "FSP_0.0.1\0";
		m_socket.getOutputStream().write(version.getBytes());
	}

	private void auth() throws IOException {
		LoginRequest.Builder builder = LoginRequest.newBuilder();
		builder.setAppId(m_app_key);
		builder.setAppSecret(m_app_secret);
		LoginRequest request = builder.build();
		write(request.toByteArray());

		LoginResponse response = LoginResponse.parseFrom(getNextMessage());
		System.out.println(response.getResultType().toString());
	}

	private void register() {
		try {
			WorkerRegisterRequest.Builder builder = WorkerRegisterRequest
					.newBuilder();
			builder.setWorkerId(m_worker_setting.getWorkerId());
			builder.setWorkerName(m_worker_setting.getWorkerName());
			builder.addExecuteTime(m_worker_setting.getExecuteTime()); // fire every 5 seconds
			builder.setTimeout(m_worker_setting.getTimeout());
			builder.setExecuteLastExpired(ExecuteLastExpiredType.IGNORE);

			write(builder.build().toByteArray());

			WorkerRegisterResponse response = WorkerRegisterResponse
					.parseFrom(getNextMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void startListen() throws IOException, InterruptedException {
		TaskNotify notify = TaskNotify.parseFrom(getNextMessage());
		Task task = new Task(notify.getTaskId(), notify.getWorkerId());
		
		m_callback.OnNotify(this, task);

	}

	public void sendTaskStart(Task task) throws IOException {
		TaskStatusUpdate.Builder builder = TaskStatusUpdate.newBuilder();
		builder.setTaskId(task.getId());
		builder.setWorkerId(task.getWorkerId());
		builder.setStatus(Status.START);
		write(builder.build().toByteArray());
	}

	public void sendTaskRunning(Task task) throws IOException {
		TaskStatusUpdate.Builder builder = TaskStatusUpdate.newBuilder();
		builder.setWorkerId(task.getWorkerId());
		builder.setTaskId(task.getId());
		builder.setStatus(Status.RUNNING);
		write(builder.build().toByteArray());
	}

	public void sendTaskComplete(Task task) throws IOException {
		TaskStatusUpdate.Builder builder = TaskStatusUpdate.newBuilder();
		builder.setWorkerId(task.getWorkerId());
		builder.setTaskId(task.getId());
		builder.setStatus(Status.STOP);
		write(builder.build().toByteArray());
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

	private void write(byte[] messageBuffer) throws IOException {
		synchronized(m_iolock){
			int size = messageBuffer.length;
			byte[] sizeBuffer = ByteBuffer.allocate(4).putInt(size).array();
			m_socket.getOutputStream().write(sizeBuffer);
			m_socket.getOutputStream().write(messageBuffer);
		}
	}
}
