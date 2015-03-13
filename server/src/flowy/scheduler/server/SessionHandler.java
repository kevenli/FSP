package flowy.scheduler.server;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;

import flowy.scheduler.server.messages.ConnectResponse;
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
        
        if (message.getMessageType() == MessageType.ConnectRequest){
        	String host = ((InetSocketAddress)ctx.channel().remoteAddress()).getAddress().getHostAddress();
            int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
            logger.debug(String.format("host:%s port:%d", host, port));
            ackConnection(ctx);
        	//m_session = SessionManager.getInstance().newSession(this);
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
}
