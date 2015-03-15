package flowy.scheduler.javasdk;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;

public class ClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

	private Client client;
	public ClientHandlerInitializer(Client client) {
		this.client = client;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		
		ch.pipeline().addLast(
				new StringDecoder(), 
				new ProtobufVarint32LengthFieldPrepender(),
				new ProtobufEncoder(),
				new ClientHandler(client));
	}

}
