package flowy.scheduler.server;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

import flowy.scheduler.entities.Application;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.ConnectResponse;
import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.Response;
import flowy.scheduler.protocal.Messages.LoginResponse.LoginResultType;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
import flowy.scheduler.protocal.Messages.Response.ResponseType;
import flowy.scheduler.protocal.Messages.ResumeSessionRequest;
import flowy.scheduler.protocal.Messages.ResumeSessionResponse;
import flowy.scheduler.protocal.Messages.ResumeSessionResponse.ResumeResultType;
import flowy.scheduler.server.data.ApplicationDAO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class SessionHandler extends ChannelHandlerAdapter {

	private static Logger logger = Logger.getLogger(SessionHandler.class);

	private Session session;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws SchedulerException {
		Request request = (Request) msg;
		logger.debug(request);

		// the handler will handle ConnectRequest & LoginRequest without
		// Session.
		if (request.getType() == RequestType.CONNECT_REQUEST) {
			ackConnection(ctx);
		} else if (request.getType() == RequestType.LOGIN_REQUEST) {
			doLogin(ctx, request.getExtension(Messages.loginRequest));
		} else if (request.getType() == RequestType.RESUME_SESSION_REQUEST) {
			resumeSession(ctx,
					request.getExtension(Messages.resumeSessionRequest));
		} else if (request.getType() == RequestType.REGISTER_TASK) {
			session.onRegisterTask(ctx,
					request.getExtension(Messages.registerTask));
		} else if (request.getType() == RequestType.TASK_STATUS_UPDATE) {
			session.onTaskStatusUpdate(ctx,
					request.getExtension(Messages.taskStatusUpdate));
		} else if (request.getType() == RequestType.LOGOUT_REQUEST) {
			SessionManager.getInstance().sessionLogout(session);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.ALL_IDLE) {
				SessionManager.getInstance().sessionTimeout(session);
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		SessionManager.getInstance().sessionTimeout(session);
		super.channelInactive(ctx);
	}

	private void ackConnection(ChannelHandlerContext ctx) {
		InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel()
				.remoteAddress();
		String host = remoteAddress.getAddress().getHostAddress();
		int port = remoteAddress.getPort();
		logger.debug(String.format("new connection, host:%s port:%d", host,
				port));

		ConnectResponse ackMessage = ConnectResponse.newBuilder().build();
		Response response = Response.newBuilder()
				.setType(ResponseType.CONNECT_RESPONSE)
				.setExtension(Messages.connectResponse, ackMessage).build();
		ctx.writeAndFlush(response);
	}

	private void resumeSession(ChannelHandlerContext ctx,
			ResumeSessionRequest request) {
		this.session = SessionManager.getInstance().resumeSession(session,
				request.getSessionId(), ctx.channel());
	}

	private void doLogin(ChannelHandlerContext ctx, LoginRequest request) {
		ApplicationDAO dao = new ApplicationDAO();
		String appKey = request.getAppKey();
		String appSecret = request.getAppSecret();
		Application application = dao.getApplication(appKey);

		if (application == null || !application.getAppSecret().equals(appSecret)) {
			// auth failed
			LoginResponse loginResponse = LoginResponse.newBuilder()
					.setResultType(LoginResultType.FAILED)
					.build();
			sendLoginResponse(ctx, loginResponse);
			return;
		} else {
			// authentication passed, bind session
			// get remote address
			Channel channel = ctx.channel();
			InetSocketAddress remoteAddress = (InetSocketAddress)channel.remoteAddress();
			String remoteHost = remoteAddress.getAddress().getHostAddress();

			Session session = SessionManager.getInstance().newSession(
					application.getId(), remoteHost, ctx.channel());
			this.session = session;

			LoginResponse loginResponse = LoginResponse.newBuilder()
					.setResultType(LoginResultType.SUCCESS)
					.setSessionId(session.getId()).build();
			sendLoginResponse(ctx, loginResponse);
			return;
		}
	}

	private void sendLoginResponse(ChannelHandlerContext ctx,
			LoginResponse loginResponse) {
		Response response = Response.newBuilder()
				.setType(ResponseType.LOGIN_RESPONSE)
				.setExtension(Messages.loginResponse, loginResponse).build();
		ctx.writeAndFlush(response);
	}

	private void sendResumeSessionResponse(ChannelHandlerContext ctx,
			ResumeSessionResponse resumeResponse) {
		Response response = Response.newBuilder()
				.setType(ResponseType.RESUME_SESSION_RESPONSE)
				.setExtension(Messages.resumeSessionResponse, resumeResponse)
				.build();
		ctx.writeAndFlush(response);
	}
}
