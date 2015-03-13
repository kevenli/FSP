package flowy.scheduler.server.messages;

import flowy.scheduler.server.exceptions.MessageInvalidException;


public abstract class Message {

	MessageType messageType;
	
	public MessageType getMessageType(){
		return messageType;
	}
	
	protected Message(MessageType messageType){
		this.messageType = messageType;
	}
	
	public abstract void parseFrom(byte[] bytes) throws MessageInvalidException;
	
	public abstract byte[] serialize();
}
