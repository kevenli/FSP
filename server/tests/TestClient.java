import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.protobuf.InvalidProtocolBufferException;

import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStart;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest;
import flowy.scheduler.protocal.Messages.WorkerRegisterResponse;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest.ExecuteLastExpiredType;

public class TestClient {
	private Socket m_socket;

	public static void main(String[] args) {
		Socket s = null;
		try {
			s = new Socket(InetAddress.getByName("localhost"), 3092);
			TestClient client = new TestClient(s);
			client.Run();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TestClient(Socket socket) {
		m_socket = socket;
	}

	public void Run() throws IOException, InterruptedException {
		SendVersion();
		Auth();
		Register();
		while (true) {
			StartListen();
		}
	}
	
	public void SendVersion() throws IOException {
		String version = "FSP_0_0_1";
		byte[] sendBuffer = version.getBytes(Charset.forName("Ascii"));
		m_socket.getOutputStream().write(sendBuffer);
		
		byte[] stopByte = new byte[1];
		stopByte[0] = 0x00;
		m_socket.getOutputStream().write(stopByte);
	}

	public void Auth() throws IOException {
		LoginRequest.Builder builder = LoginRequest.newBuilder();
		builder.setAppKey("123");
		builder.setAppSecret("321");
		LoginRequest request = builder.build();
		write(request.toByteArray());

		LoginResponse response = LoginResponse.parseFrom(getNextMessage());
		System.out.println(response.getResultType().toString());
	}

	public void Register() {
		try {
			WorkerRegisterRequest.Builder builder = WorkerRegisterRequest
					.newBuilder();
			builder.setWorkerId("123456789");
			builder.setWorkerName("Test Worder");
			builder.addExecuteTime("0/5 * * * * ?"); // fire every 5 seconds
			builder.setTimeout(60);
			builder.setExecuteLastExpired(ExecuteLastExpiredType.RUN);

			write(builder.build().toByteArray());

			WorkerRegisterResponse response = WorkerRegisterResponse
					.parseFrom(getNextMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void StartListen() throws IOException, InterruptedException {
		TaskNotify notify = TaskNotify.parseFrom(getNextMessage());
		System.out.println("Notify received, start to run");
		sendTaskStart(notify.getTaskId());
		Thread.sleep(1000);
		sendTaskRunning(notify.getTaskId());
		Thread.sleep(1000);
		sendTaskRunning(notify.getTaskId());
		Thread.sleep(1000);

		sendTaskComplete(notify.getTaskId());

	}

	private void sendTaskStart(String taskId) throws IOException {
		TaskStatusUpdate.Builder builder = TaskStatusUpdate.newBuilder();
		builder.setTaskId(taskId);
		builder.setWorkerId("");
		builder.setStatus(Status.START);
		write(builder.build().toByteArray());
	}

	private void sendTaskRunning(String taskId) throws IOException {
		TaskStatusUpdate.Builder builder = TaskStatusUpdate.newBuilder();
		builder.setWorkerId("");
		builder.setTaskId(taskId);
		builder.setStatus(Status.RUNNING);
		write(builder.build().toByteArray());
	}

	private void sendTaskComplete(String taskId) throws IOException {
		TaskStatusUpdate.Builder builder = TaskStatusUpdate.newBuilder();
		builder.setWorkerId("");
		builder.setTaskId(taskId);
		builder.setStatus(Status.STOP);
		write(builder.build().toByteArray());
	}

	private byte[] getNextMessage() throws IOException {
		byte[] sizeBuffer = new byte[4];
		m_socket.getInputStream().read(sizeBuffer, 0, 4);
		int size = ByteBuffer.wrap(sizeBuffer).getInt();
		byte[] buffer = new byte[size];
		m_socket.getInputStream().read(buffer, 0, size);
		return buffer;
	}

	private void write(byte[] messageBuffer) throws IOException {
		int size = messageBuffer.length;
		byte[] sizeBuffer = ByteBuffer.allocate(4).putInt(size).array();
		m_socket.getOutputStream().write(sizeBuffer);
		m_socket.getOutputStream().write(messageBuffer);
	}

}
