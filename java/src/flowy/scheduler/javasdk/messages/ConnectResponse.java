package flowy.scheduler.javasdk.messages;

import com.google.protobuf.InvalidProtocolBufferException;

import flowy.scheduler.javasdk.exceptions.MessageInvalidException;

public class ConnectResponse extends Message {

			
	public ConnectResponse() {
		super(MessageType.ConnectResponse);
	}

	@Override
	public void parseFrom(byte[] bytes) throws MessageInvalidException {
		try {
			@SuppressWarnings("unused")
			flowy.scheduler.protocal.Messages.ConnectResponse innerMessage = 
					flowy.scheduler.protocal.Messages.ConnectResponse.parseFrom(bytes);
		} catch (InvalidProtocolBufferException e) {
			throw new MessageInvalidException();
		}

	}

	@Override
	public byte[] serialize() {
		flowy.scheduler.protocal.Messages.ConnectResponse.Builder builder = 
				flowy.scheduler.protocal.Messages.ConnectResponse.newBuilder();
		
		return builder.build().toByteArray();
	}
	
	

}
