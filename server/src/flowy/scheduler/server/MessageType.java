package flowy.scheduler.server;

public enum MessageType {

	// upgoing messages:
	HeartBeat(0x00),
	LoginRequest(0x01),
	ReconnectRequest(0x02),
	TaskStart(0x03),
	TaskProgress(0x04),
	TaskComplete(0x05),
	TaskFail(0x06),
	
	
	// down going messages:
	HeartbeatResponse(0x80),
	LoginResposne(0x81),
	RegisterReponse(0x82),
	TaskNotify(0x83);
	

	private int value;

	private MessageType(int value) {
		this.value = value;
	}
	
	public int getValue(){
		return this.value;
	}
}
