package flowy.scheduler.javasdk;

import java.util.List;

import flowy.scheduler.javasdk.exceptions.MessageInvalidException;
import flowy.scheduler.javasdk.messages.ConnectRequest;
import flowy.scheduler.javasdk.messages.ConnectResponse;
import flowy.scheduler.javasdk.messages.Message;
import flowy.scheduler.javasdk.messages.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws MessageInvalidException {
		if (in.readableBytes() < 4) {
			return;
		}

		in.markReaderIndex();
		int length = in.readInt();

		if (in.readableBytes() < length) {
			in.resetReaderIndex();
			return;
		}

		MessageType messageTypeBytes = MessageType.fromShort(in.readShort());
		byte[] messageBodyBuffer = new byte[length - 2];

		in.readBytes(messageBodyBuffer);

		Message message = null;
		switch (messageTypeBytes) {
		case ConnectRequest:
			message = new ConnectRequest();
			message.parseFrom(messageBodyBuffer);
			break;
		case ConnectResponse:
			message = new ConnectResponse();
			message.parseFrom(messageBodyBuffer);
			break;
		default:
			throw new MessageInvalidException();
		}

		out.add(message);
	}

}
