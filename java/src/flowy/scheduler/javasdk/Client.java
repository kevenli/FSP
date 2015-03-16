package flowy.scheduler.javasdk;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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

import flowy.scheduler.javasdk.exceptions.AuthenticationFailException;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.LoginResponse.LoginResultType;
import flowy.scheduler.protocal.Messages.RegisterTask;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
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
	
	private Exception connectionException = null;
	
	private Exception authenticationException = null;
	
	// lock this object when connecting and authenticating
	private Object connectLock = new Object(); 
	
	private Channel channel;

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

	public void connect() throws Exception {
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
	
	public void registerTask(Task task){
		RegisterTask registerTaskRequest = RegisterTask.newBuilder()
				.setTaskId(task.getId())
				.setExecuteTime(task.getExecuteTime()).build();
		
		Request request = Request.newBuilder()
				.setType(RequestType.REGISTER_TASK)
				.setExtension(Messages.registerTask, registerTaskRequest).build();
		
		this.channel.writeAndFlush(request);
	}

	void login(ChannelHandlerContext ctx) {
		LoginRequest loginRequest = LoginRequest.newBuilder()
				.setAppKey(this.m_app_key)
				.setAppSecret(this.m_app_secret).build();
		
		Request request = Request.newBuilder()
				.setType(Request.RequestType.LOGIN_REQUEST)
				.setExtension(Messages.loginRequest, loginRequest).build();

		ctx.writeAndFlush(request);
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
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, 0L, TimeUnit.SECONDS);
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
}
