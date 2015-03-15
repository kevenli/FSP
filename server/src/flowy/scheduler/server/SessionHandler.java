package flowy.scheduler.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.log4j.Logger;

import flowy.scheduler.protocal.Messages;
import flowy.scheduler.protocal.Messages.ConnectResponse;
import flowy.scheduler.protocal.Messages.LoginRequest;
import flowy.scheduler.protocal.Messages.LoginResponse;
import flowy.scheduler.protocal.Messages.Response;
import flowy.scheduler.protocal.Messages.LoginResponse.LoginResultType;
import flowy.scheduler.protocal.Messages.Request;
import flowy.scheduler.protocal.Messages.Request.RequestType;
import flowy.scheduler.protocal.Messages.Response.ResponseType;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class SessionHandler extends ChannelHandlerAdapter {
	
	private Session m_session = null;
	
	private static Logger logger = Logger.getLogger(SessionHandler.class);
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Request request = (Request)msg;
        logger.debug(request);
        
        // the handler will handle ConnectRequest & LoginRequest without Session.
        if (request.getType() == RequestType.CONNECT_REQUEST){
        	InetSocketAddress remoteAddress = (InetSocketAddress)ctx.channel().remoteAddress();
        	String host = remoteAddress.getAddress().getHostAddress();
            int port = remoteAddress.getPort();
            logger.debug(String.format("host:%s port:%d", host, port));
            ackConnection(ctx);
        	return;
        }
        //m_session.handleMessage(message);
    }
	
	@Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            }
        }
    }
	
	private void ackConnection(ChannelHandlerContext ctx){
		ConnectResponse ackMessage = ConnectResponse.newBuilder()
				.build();
		Response response = Response.newBuilder()
				.setType(ResponseType.CONNECT_RESPONSE)
				.setExtension(Messages.connectResponse, ackMessage).build();
		ctx.writeAndFlush(response);
	}
	
	private void doLogin(ChannelHandlerContext ctx, LoginRequest request){
		if (request.getAppKey() != "abc" || request.getAppSecret() != "123"){
			// auth failed 
			LoginResponse response = LoginResponse.newBuilder()
					.setResultType(LoginResultType.FAILED).build();
			ctx.writeAndFlush(response);
			return;
		} else{
			LoginResponse response = LoginResponse.newBuilder()
				.setResultType(LoginResultType.FAILED).build();
			ctx.writeAndFlush(response);
			return;
		}
	}
}
