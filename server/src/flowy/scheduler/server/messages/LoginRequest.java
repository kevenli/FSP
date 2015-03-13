package flowy.scheduler.server.messages;

import com.google.protobuf.InvalidProtocolBufferException;

import flowy.scheduler.server.exceptions.MessageInvalidException;

public class LoginRequest extends Message {
	private String appKey;
	private String appSecret;
	
	public LoginRequest() {
		super(MessageType.LoginRequest);
	}
	
	public LoginRequest(String appKey, String appSecret){
		super(MessageType.LoginRequest);
		this.appKey = appKey;
		this.appSecret = appSecret;
	}
	
	public String getAppKey(){
		return appKey;
	}
	
	public String getAppSecret(){
		return appSecret;
	}

	@Override
	public void parseFrom(byte[] data) throws MessageInvalidException {
		try {
			flowy.scheduler.protocal.Messages.LoginRequest innerMessage = 
					flowy.scheduler.protocal.Messages.LoginRequest.parseFrom(data);
			this.appKey = innerMessage.getAppKey();
			this.appSecret = innerMessage.getAppSecret();
		} catch (InvalidProtocolBufferException e) {
			throw new MessageInvalidException();
		}
		
	}

	@Override
	public byte[] serialize() {
		flowy.scheduler.protocal.Messages.LoginRequest.Builder builder =
				flowy.scheduler.protocal.Messages.LoginRequest.newBuilder();
		
		builder.setAppKey(appKey);
		builder.setAppSecret(appSecret);
		return builder.build().toByteArray();
	}

}
