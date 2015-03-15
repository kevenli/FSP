package flowy.scheduler.javasdk;

import flowy.scheduler.javasdk.exceptions.MessageInvalidException;
import flowy.scheduler.javasdk.messages.ConnectResponse;
import flowy.scheduler.javasdk.messages.Message;
import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.ConnectRequest;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class ClientHandler extends ChannelHandlerAdapter {
	
	private Client client;
	public ClientHandler(Client client){
		this.client = client;
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws MessageInvalidException {
        Message message = (Message)msg;
        switch(message.getMessageType()){
		case ConnectResponse:
			onConnectResponse(ctx, (ConnectResponse)message);
			break;
		case HeartBeat:
			break;
		case HeartbeatResponse:
			break;
		case LoginRequest:
			break;
		case LoginResposne:
			break;
		case ReconnectRequest:
			break;
		case RegisterReponse:
			break;
		case TaskComplete:
			break;
		case TaskFail:
			break;
		case TaskNotify:
			break;
		case TaskProgress:
			break;
		case TaskStart:
			break;
		case Unkown:
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
		requestBuilder.setExtension(Messages.connect, connectRequest);
		requestBuilder.setType(RequestType.CONNECT);
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
