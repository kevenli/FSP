package flowy.scheduler.javasdk;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ClientHandlerInitializer extends ChannelInitializer<SocketChannel> {

	private Client client;
	public ClientHandlerInitializer(Client client) {
		this.client = client;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
				new MessageDecoder(), 
				new MessageEncoder(), 
				new ClientHandler(client));
	}

}
