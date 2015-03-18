package flowy.scheduler.javasdk;

import flowy.scheduler.javasdk.exceptions.MessageInvalidException;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.ConnectRequest;
import flowy.scheduler.protocal.Messages.ConnectResponse;
import flowy.scheduler.protocal.Messages.Heartbeat;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
import flowy.scheduler.protocal.Messages.Response;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientHandler extends ChannelHandlerAdapter {
	
	private Client client;
	public ClientHandler(Client client){
		this.client = client;
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws MessageInvalidException {
        Response response = (Response)msg;
        switch(response.getType()){
		case CONNECT_RESPONSE:
			onConnectResponse(ctx, response.getExtension(Messages.connectResponse));
			break;
		case LOGIN_RESPONSE:
			client.onLoginResponse(ctx, response.getExtension(Messages.loginResponse));
			break;
		case REGISTER_TASK_RESPONSE:
			client.onTaskRegisterResponse(response.getExtension(Messages.registerTaskResponse));
			break;
		case TASK_NOTIFY:
			client.onTaskNotify(response.getExtension(Messages.taskNotify));
			break;
		case LOGOUT_RESPONSE:
			client.onLogoutResponse(response.getExtension(Messages.logoutResponse));
		default:
			throw new MessageInvalidException(); 
        }
    }
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx){
		
		ConnectRequest connectRequest = ConnectRequest.newBuilder()
				.setClientProtocolVersion("FSP_0.0.1").build();
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setExtension(Messages.connectRequest, connectRequest);
		requestBuilder.setType(RequestType.CONNECT_REQUEST);
		ctx.writeAndFlush(requestBuilder.build());
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception{
		client.onDisconnect(ctx);
		super.channelInactive(ctx);
	}
	
	private void onConnectResponse(ChannelHandlerContext ctx, ConnectResponse msg){
		client.login(ctx);
	}
	
	@Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                this.sendHeartbeat(ctx);
            }
        }
    }
	
	private void sendHeartbeat(ChannelHandlerContext ctx){
		Heartbeat heartbeat = Heartbeat.newBuilder().build();
		Request.Builder requestBuilder = Request.newBuilder();
		requestBuilder.setExtension(Messages.heartbeat, heartbeat);
		requestBuilder.setType(RequestType.HEARTBEAT);
		
		ctx.writeAndFlush(requestBuilder.build());
	}
}
