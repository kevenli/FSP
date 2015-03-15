package flowy.scheduler.javasdk;

import com.google.protobuf.ExtensionRegistry;

import flowy.scheduler.protocal.Messages;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;

public class ClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

	private Client client;
	public ClientHandlerInitializer(Client client) {
		this.client = client;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		final ExtensionRegistry registry = ExtensionRegistry.newInstance();
        Messages.registerAllExtensions(registry);
        
		ch.pipeline().addLast(
				new ProtobufVarint32FrameDecoder(), 
				new ProtobufDecoder(
                        Messages.Response.getDefaultInstance(), registry),
				new ProtobufVarint32LengthFieldPrepender(),
				new ProtobufEncoder(),
				new ClientHandler(client));
	}

}
