package flowy.scheduler.javasdk;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import flowy.scheduler.javasdk.exceptions.AuthenticationFailException;
import flowy.scheduler.javasdk.exceptions.TaskAlreadyExistsException;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.LogoutResponse;
import flowy.scheduler.protocal.Messages.RegisterTask;
import flowy.scheduler.protocal.Messages.RegisterTaskResponse;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
import flowy.scheduler.protocal.Messages.TaskNotify;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate;
import flowy.scheduler.protocal.Messages.TaskStatusUpdate.Status;

public class Client {
	public static int DEFAULT_HOST_PORT = 3092;

	private String[] hosts;

	private String appKey;

	private String appSecret;

	private boolean isShutdown = false;

	private EventLoopGroup workerGroup;

	private ChannelFuture channelFuture;
	
	private Exception authenticationException = null;
	
	// lock this object when connecting and authenticating
	private Object connectLock = new Object(); 
	
	private Object registerTaskLock = new Object();
	private TaskAlreadyExistsException registerTaskException;
	
	private Channel channel;
	
	private HashMap<String, ITaskNotifyCallback> taskCallbacks = new HashMap<String, ITaskNotifyCallback>();
	private HashMap<String, Task> tasks = new HashMap<String, Task>();
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(10);

	public Client(String hosts, String app_key, String app_secret) {
		this.hosts = hosts.split(";");
		appKey = app_key;
		appSecret = app_secret;
	}

	private SocketAddress pickRemoteAddress() {
		String host = hosts[0];

		String[] host_parts = host.split(":");
		String host_name = host_parts[0];
		int host_port = DEFAULT_HOST_PORT;
		if (host_parts.length > 0) {
			host_port = Integer.parseInt(host_parts[1]);
		}

		return InetSocketAddress.createUnresolved(host_name, host_port);
	}

	public void connect() throws Exception {
		SocketAddress remoteAddress = pickRemoteAddress();

		if (workerGroup == null) {
			workerGroup = new NioEventLoopGroup(2);
		}

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

		bootstrap.handler(new ClientHandlerInitializer(this));

		channelFuture = bootstrap.connect(remoteAddress);
		channelFuture.addListener(new ConnectionListener(this));
		channelFuture.sync();
		this.channel = channelFuture.channel();
		
		// lock connectLock, wait for notification after login
		synchronized(connectLock){
			connectLock.wait();
		}
		
		if (authenticationException!=null){
			this.isShutdown = true;
			throw new AuthenticationFailException();
		}
	}
	
	private void reconnect(){
		SocketAddress remoteAddress = pickRemoteAddress();

		if (workerGroup == null) {
			workerGroup = new NioEventLoopGroup(2);
		}

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(workerGroup);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

		bootstrap.handler(new ClientHandlerInitializer(this));

		channelFuture = bootstrap.connect(remoteAddress);
		channelFuture.addListener(new ConnectionListener(this));
		try {
			channelFuture.sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.channel = channelFuture.channel();
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
			channelFuture.channel().closeFuture().sync();
			reconnect();
		}
	}
	
	public void registerTask(Task task, ITaskNotifyCallback callback) throws InterruptedException, TaskAlreadyExistsException{
		RegisterTask registerTaskRequest = RegisterTask.newBuilder()
				.setTaskId(task.getId())
				.setExecuteTime(task.getExecuteTime()).build();
		
		Request request = Request.newBuilder()
				.setType(RequestType.REGISTER_TASK)
				.setExtension(Messages.registerTask, registerTaskRequest).build();
		
		synchronized(registerTaskLock){ 
			this.channel.writeAndFlush(request);
			registerTaskLock.wait();
		}
		if (registerTaskException != null){
			throw registerTaskException;
		}
		taskCallbacks.put(task.getId(), callback);
		tasks.put(task.getId(), task);
	}

	void login(ChannelHandlerContext ctx) {
		LoginRequest loginRequest = LoginRequest.newBuilder()
				.setAppKey(this.appKey)
				.setAppSecret(this.appSecret).build();
		
		Request request = Request.newBuilder()
				.setType(Request.RequestType.LOGIN_REQUEST)
				.setExtension(Messages.loginRequest, loginRequest).build();

		ctx.writeAndFlush(request);
	}

	public void onDisconnect(ChannelHandlerContext ctx)
			throws InterruptedException {
		final Client client = this;
		final EventLoop eventLoop = ctx.channel().eventLoop();
		if(!this.isShutdown){
			eventLoop.schedule(new Runnable() {
				@Override
				public void run() {
					try {
						client.reconnect();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, 0L, TimeUnit.SECONDS);
		}
	}
	
	public void onLoginResponse(ChannelHandlerContext ctx, LoginResponse loginResponse){
		switch (loginResponse.getResultType()){
		case FAILED:
			authenticationException = new AuthenticationFailException();
			break;
		case SUCCESS:
			authenticationException = null;
			break;
		}
		synchronized(connectLock){
			connectLock.notify();
		}
	}
	
	public void onTaskRegisterResponse(RegisterTaskResponse registerTaskResponse){
		synchronized(registerTaskLock){
			if(registerTaskResponse.getRegisterResult() != RegisterTaskResponse.RegisterTaskResultType.SUCCESS){
				registerTaskException = new TaskAlreadyExistsException();
			}
			registerTaskLock.notify();
		}
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
								client.reconnect();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}, 1L, TimeUnit.SECONDS);
				}
			}
		}
	}

	public void close() throws InterruptedException {
		this.isShutdown = true;
		this.channel.close();
		workerGroup.shutdownGracefully();
		workerGroup = null;
	}

	public void onTaskNotify(TaskNotify notify) {
		
		String taskId = notify.getTaskId();
		ITaskNotifyCallback callback = taskCallbacks.get(taskId);
		Task task = tasks.get(taskId);
		callback.onTaskNotify(this, task, notify.getTaskInstanceId());
		threadPool.execute(new TaskNotifyCallbackRunner(this, task, notify.getTaskInstanceId(), callback));
	}
	
	public void taskStart(String taskInstanceId){
		TaskStatusUpdate taskStatusUpdate = TaskStatusUpdate.newBuilder()
				.setInstanceId(taskInstanceId)
				.setStatus(Status.START)
				.build();
		
		Request request = Request.newBuilder()
				.setType(Request.RequestType.TASK_STATUS_UPDATE)
				.setExtension(Messages.taskStatusUpdate, taskStatusUpdate).build();

		this.channel.writeAndFlush(request);
	}
	
	public void taskRunning(String taskInstanceId, int percentage){
		TaskStatusUpdate taskStatusUpdate = TaskStatusUpdate.newBuilder()
				.setInstanceId(taskInstanceId)
				.setStatus(Status.RUNNING)
				.setPercentage(percentage)
				.build();
		
		Request request = Request.newBuilder()
				.setType(Request.RequestType.TASK_STATUS_UPDATE)
				.setExtension(Messages.taskStatusUpdate, taskStatusUpdate).build();

		this.channel.writeAndFlush(request);
	}
	
	public void taskComplete(String taskInstanceId){
		TaskStatusUpdate taskStatusUpdate = TaskStatusUpdate.newBuilder()
				.setInstanceId(taskInstanceId)
				.setStatus(Status.COMPLETE)
				.build();
		
		Request request = Request.newBuilder()
				.setType(Request.RequestType.TASK_STATUS_UPDATE)
				.setExtension(Messages.taskStatusUpdate, taskStatusUpdate).build();

		this.channel.writeAndFlush(request);
	}
	
	public void taskFail(String taskInstanceId, String errorMessage){
		TaskStatusUpdate taskStatusUpdate = TaskStatusUpdate.newBuilder()
				.setInstanceId(taskInstanceId)
				.setStatus(Status.FAILED)
				.setErrorMessage(errorMessage)
				.build();
		
		Request request = Request.newBuilder()
				.setType(Request.RequestType.TASK_STATUS_UPDATE)
				.setExtension(Messages.taskStatusUpdate, taskStatusUpdate).build();

		this.channel.writeAndFlush(request);
	}

	public void onLogoutResponse(LogoutResponse extension) {
		this.channel.close();
	}
}
