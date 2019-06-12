package flowy.scheduler.server;

import com.google.protobuf.ExtensionRegistry;

import flowy.scheduler.protocal.Messages;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

public class SessionHandlerInitializer extends ChannelInitializer<SocketChannel> {

	public static final int SESSION_TIMEOUT = 10;
	
	public SessionHandlerInitializer() {

	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		final ExtensionRegistry registry = ExtensionRegistry.newInstance();
        Messages.registerAllExtensions(registry);
		
		 ChannelPipeline pipeline = ch.pipeline();
    	 pipeline.addLast(
    			 new IdleStateHandler(0, 0, SESSION_TIMEOUT),
    			 new ProtobufVarint32LengthFieldPrepender(),
                 new ProtobufEncoder(), 
    			 new ProtobufVarint32FrameDecoder(),
    			 new ProtobufDecoder(Messages.Request.getDefaultInstance(), registry),
    			 new SessionHandler());
	}

}
