package flowy.scheduler.javasdk.messages;

import flowy.scheduler.javasdk.exceptions.MessageInvalidException;


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
