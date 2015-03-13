package flowy.scheduler.javasdk;

import flowy.scheduler.javasdk.messages.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {

	public MessageEncoder() {

	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out)
			throws Exception {
		
		byte[] buffer = msg.serialize();
		out.writeInt(buffer.length+2);
		out.writeShort(msg.getMessageType().getValue());
		out.writeBytes(buffer);
		
	}

}
