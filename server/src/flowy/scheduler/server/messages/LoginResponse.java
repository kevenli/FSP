package flowy.scheduler.server.messages;

import com.google.protobuf.InvalidProtocolBufferException;

import flowy.scheduler.server.exceptions.MessageInvalidException;

public class LoginResponse extends Message {

	public enum LoginResultType{
		Success,
		Fail,
	}
	
	private LoginResultType resultType;
	
	private String errorMessage;
	
	private int sessionId;
	
	public LoginResponse(LoginResultType resultType){
		this(resultType, null);
	}
	
	public LoginResponse(LoginResultType resultType, String errorMessage){
		super(MessageType.LoginResposne);
		this.resultType = resultType;
		this.errorMessage = errorMessage;
	}
	
	public LoginResponse(LoginResultType resultType, int sessionId){
		super(MessageType.LoginResposne);
		this.resultType = resultType;
		this.sessionId = sessionId;
	}

	@Override
	public void parseFrom(byte[] bytes) throws MessageInvalidException {
		try {
			@SuppressWarnings("unused")
			flowy.scheduler.protocal.Messages.LoginResponse innerMessage = 
					flowy.scheduler.protocal.Messages.LoginResponse.parseFrom(bytes);
			// TODO deserialize
		} catch (InvalidProtocolBufferException e) {
			throw new MessageInvalidException();
		}
	}

	@Override
	public byte[] serialize() {
		// TODO Auto-generated method stub
		return null;
	}

}
