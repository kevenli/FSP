package flowy.scheduler.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.log4j.Logger;

import flowy.scheduler.server.messages.ConnectResponse;
import flowy.scheduler.server.messages.LoginRequest;
import flowy.scheduler.server.messages.LoginResponse;
import flowy.scheduler.server.messages.LoginResponse.LoginResultType;
import flowy.scheduler.server.messages.Message;
import flowy.scheduler.server.messages.MessageType;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class SessionHandler extends ChannelHandlerAdapter {
	
	private Session m_session = null;
	
	private static Logger logger = Logger.getLogger(SessionHandler.class);
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Message message = (Message)msg;
        logger.debug(message);
        
        // the handler will handle ConnectRequest & LoginRequest without Session.
        if (message.getMessageType() == MessageType.ConnectRequest){
        	InetSocketAddress remoteAddress = (InetSocketAddress)ctx.channel().remoteAddress();
        	String host = remoteAddress.getAddress().getHostAddress();
            int port = remoteAddress.getPort();
            logger.debug(String.format("host:%s port:%d", host, port));
            ackConnection(ctx);
        	return;
        }
        else if (message.getMessageType() == MessageType.LoginRequest){
        	m_session = SessionManager.getInstance().newSession(this);
        	LoginRequest request = (LoginRequest)message;
        	
        	return;
        }
        
        
        m_session.handleMessage(message);
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
		ConnectResponse ackMessage = new ConnectResponse();
		ctx.writeAndFlush(ackMessage);
	}
	
	private void doLogin(ChannelHandlerContext ctx, LoginRequest request){
		if (request.getAppKey() != "abc" || request.getAppSecret() != "123"){
			// auth failed 
			LoginResponse response = new LoginResponse(LoginResultType.Fail);
			ctx.writeAndFlush(response);
			return;
		} else{
			LoginResponse response = new LoginResponse(LoginResultType.Success);
			ctx.writeAndFlush(response);
			return;
		}
		
	}
}
