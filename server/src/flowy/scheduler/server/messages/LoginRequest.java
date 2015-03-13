package flowy.scheduler.server.messages;

import com.google.protobuf.InvalidProtocolBufferException;

import flowy.scheduler.protocal.Messages.LoginRequest.ConnectionType;
import flowy.scheduler.server.exceptions.MessageInvalidException;

public class LoginRequest extends Message {

	public enum ConnectType{
		NEW,
		RECONNECT
	}
	
	private String appKey;
	private String appSecret;
	private ConnectType connectType = ConnectType.NEW;
	private int sessionId = -1;
	
	public LoginRequest() {
		super(MessageType.LoginRequest);
	}
	
	public LoginRequest(String appKey, String appSecret){
		super(MessageType.LoginRequest);
		this.appKey = appKey;
		this.appSecret = appSecret;
	}
	
	public LoginRequest(String appKey, 
			String appSecret, 
			ConnectType connectType, int sessionId ){
		super(MessageType.LoginRequest);
		this.appKey = appKey;
		this.appSecret = appSecret;
		this.connectType = connectType;
		this.sessionId = sessionId;
	}

	@Override
	public void parseFrom(byte[] data) throws MessageInvalidException {
		try {
			flowy.scheduler.protocal.Messages.LoginRequest innerMessage = 
					flowy.scheduler.protocal.Messages.LoginRequest.parseFrom(data);
			
			this.appKey = innerMessage.getAppKey();
			this.appSecret = innerMessage.getAppSecret();
			if (innerMessage.hasConnectionType()){
				switch (innerMessage.getConnectionType()){
				case NEW:
					this.connectType = ConnectType.NEW;
					break;
				case RECONNECT:
					this.connectType = ConnectType.RECONNECT;
					break;
				}
			}
			if (innerMessage.hasSessionId()){
				this.sessionId = innerMessage.getSessionId();
			}
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
		switch (connectType){
		case NEW:
			builder.setConnectionType(ConnectionType.NEW);
			break;
		case RECONNECT:
			builder.setConnectionType(ConnectionType.RECONNECT);
			break;
		}
		builder.setSessionId(sessionId);
		
		return builder.build().toByteArray();
	}

}
