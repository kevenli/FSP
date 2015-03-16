package flowy.scheduler.javasdk;

import flowy.scheduler.javasdk.exceptions.MessageInvalidException;
import flowy.scheduler.javasdk.messages.Message;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.ConnectRequest;
import flowy.scheduler.protocal.Messages.ConnectResponse;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
import flowy.scheduler.protocal.Messages.Response;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

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
		client.bindChannel(ctx);
		client.login(ctx);
	}
}
