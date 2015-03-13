package flowy.scheduler.server.messages;

import org.apache.log4j.Logger;

import com.google.protobuf.InvalidProtocolBufferException;

import flowy.scheduler.server.exceptions.MessageInvalidException;


public class ConnectRequest extends Message {

	private String clientProtocolVersion;
	
	private static Logger logger = Logger.getLogger(ConnectRequest.class);
	
	public ConnectRequest() {
		super(MessageType.ConnectRequest);
	}
	
	public String getClientProtocolVersion(){
		return clientProtocolVersion;
	}
	
	

	@Override
	public void parseFrom(byte[] data) throws MessageInvalidException {
		flowy.scheduler.protocal.Messages.ConnectRequest innerMessage;
		try {
			innerMessage = flowy.scheduler.protocal.Messages.ConnectRequest.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			logger.warn(e);
			throw new MessageInvalidException();
		}
		
		this.clientProtocolVersion = innerMessage.getClientProtocolVersion();
		
	}

	@Override
	public byte[] serialize() {
		flowy.scheduler.protocal.Messages.ConnectRequest.Builder builder= 
				flowy.scheduler.protocal.Messages.ConnectRequest.newBuilder();
		return builder.build().toByteArray();
	}

}
