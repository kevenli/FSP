package flowy.scheduler.server.messages;

public enum MessageType {

	
	
	// upgoing messages:
	HeartBeat((short)0x0000),
	ConnectRequest((short)0x0001),
	LoginRequest((short)0x0002),
	ReconnectRequest((short)0x0003),
	TaskStart((short)0x0004),
	TaskProgress((short)0x0005),
	TaskComplete((short)0x0006),
	TaskFail((short)0x0007),
	
	
	// down going messages:
	HeartbeatResponse((short)0x8000),
	ConnectResponse((short)0x8001),
	LoginResposne((short)0x8002),
	RegisterReponse((short)0x8003),
	TaskNotify((short)0x8004),
	
	Unkown((short)0xffff);
	

	private short value;

	private MessageType(short value) {
		this.value = value;
	}
	
	public short getValue(){
		return this.value;
	}
	
	public static MessageType fromShort(short value){
		for (MessageType item : MessageType.values()){
			if (item.getValue() == value){
				return item;
			}
		}
		return Unkown;
		
	}
}
