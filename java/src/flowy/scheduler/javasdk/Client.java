package flowy.scheduler.javasdk;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest;
import flowy.scheduler.protocal.Messages.WorkerRegisterResponse;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;
import flowy.scheduler.protocal.Messages.WorkerRegisterRequest.ExecuteLastExpiredType;

public class Client {
	public static int DEFAULT_HOST_PORT = 3092;

	private String[] m_hosts;

	private Socket m_socket;

	private IClientCallback m_callback;

	private WorkerSetting m_worker_setting;

	private String m_app_key;

	private String m_app_secret;

	private Object m_iolock = new Object();

	private boolean isShutdown = false;

	private ChannelHandlerContext m_handlerContext;

	private EventLoopGroup workerGroup;

	private ChannelFuture channelFuture;

	public Client(String hosts, String app_key, String app_secret,
			WorkerSetting workerSetting, IClientCallback callback) {

		m_hosts = hosts.split(";");
		m_callback = callback;
		m_worker_setting = workerSetting;
		m_app_key = app_key;
		m_app_secret = app_secret;
	}

	private SocketAddress pickRemoteAddress() {
		String host = m_hosts[0];

		String[] host_parts = host.split(":");
		String host_name = host_parts[0];
		int host_port = DEFAULT_HOST_PORT;
		if (host_parts.length > 0) {
			host_port = Integer.parseInt(host_parts[1]);
		}

		return InetSocketAddress.createUnresolved(host_name, host_port);
	}

	public void connect() throws InterruptedException {
		SocketAddress remoteAddress = pickRemoteAddress();

		if (workerGroup == null) {
			workerGroup = new NioEventLoopGroup();
		}

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

		bootstrap.handler(new ClientHandlerInitializer(this));

		channelFuture = bootstrap.connect(remoteAddress);
		channelFuture.addListener(new ConnectionListener(this));
		channelFuture.sync();
	}

	public void start() throws UnknownHostException, IOException,
			InterruptedException {

		// wait until connection closed
		// channelFuture.channel().closeFuture().sync();

		while (!isShutdown) {
			// // if not shutting down, reconnect automatically
			// connect();
			//
			//channelFuture.channel().closeFuture().sync();
			Thread.sleep(1l);
		}

		// m_socket = new Socket(InetAddress.getByName(host_name), host_port);

		// sendVersion();
		// auth();
		// register();
		// while (true) {
		// startListen();
		// }
	}

	private void sendVersion() throws IOException {
		String version = "FSP_0.0.1\0";
		m_socket.getOutputStream().write(version.getBytes());
	}

	private void sendConnectRequest() {

	}

	private void auth() throws IOException {
		LoginRequest.Builder builder = LoginRequest.newBuilder();
		builder.setAppKey(m_app_key);
		builder.setAppSecret(m_app_secret);
		LoginRequest request = builder.build();
		write(request.toByteArray());

		LoginResponse response = LoginResponse.parseFrom(getNextMessage());
		System.out.println(response.getResultType().toString());
	}

	void login(ChannelHandlerContext ctx) {

	}

	private void register() {
		try {
			WorkerRegisterRequest.Builder builder = WorkerRegisterRequest
					.newBuilder();
			builder.setWorkerId(m_worker_setting.getWorkerId());
			builder.setWorkerName(m_worker_setting.getWorkerName());
			builder.addExecuteTime(m_worker_setting.getExecuteTime()); // fire
																		// every
																		// 5
																		// seconds
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
		synchronized (m_iolock) {
			byte[] sizeBuffer = new byte[4];
			m_socket.getInputStream().read(sizeBuffer, 0, 4);
			int size = ByteBuffer.wrap(sizeBuffer).getInt();
			byte[] buffer = new byte[size];
			m_socket.getInputStream().read(buffer, 0, size);
			return buffer;
		}
	}

	private void write(byte[] messageBuffer) throws IOException {
		synchronized (m_iolock) {
			int size = messageBuffer.length;
			byte[] sizeBuffer = ByteBuffer.allocate(4).putInt(size).array();
			m_socket.getOutputStream().write(sizeBuffer);
			m_socket.getOutputStream().write(messageBuffer);
		}
	}

	public void bindChannel(ChannelHandlerContext ctx) {
		if (ctx != null) {
			m_handlerContext = ctx;
		}
	}

	public void onDisconnect(ChannelHandlerContext ctx)
			throws InterruptedException {
		final Client client = this;
		final EventLoop eventLoop = ctx.channel().eventLoop();
		eventLoop.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					client.connect();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0L, TimeUnit.SECONDS);
		// if not shutting down, reconnect automatically
		// SocketAddress remoteAddress = pickRemoteAddress();
		// channelFuture = ctx.connect(remoteAddress).sync();
		// channelFuture = bootstrap.connect(remoteAddress).sync();
	}

	public class ConnectionListener implements ChannelFutureListener {
		private Client client;

		public ConnectionListener(Client client) {
			this.client = client;
		}

		@Override
		public void operationComplete(ChannelFuture channelFuture)
				throws Exception {
			if (!channelFuture.isSuccess()) {
				if (!client.isShutdown) {
					System.out.println("Reconnect");
					final EventLoop loop = channelFuture.channel().eventLoop();
					loop.schedule(new Runnable() {
						@Override
						public void run() {
							try {
								client.connect();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}, 1L, TimeUnit.SECONDS);
				}
			}
		}
	}
}
