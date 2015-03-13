package flowy.scheduler.javasdk.messages;

public class ConnectRequest extends Message {

	public ConnectRequest() {
		super(MessageType.ConnectRequest);
	}

	@Override
	public void parseFrom(byte[] bytes) {
		// TODO Auto-generated method stub
		
	}
	
	public byte[] serialize(){
		flowy.scheduler.protocal.Messages.ConnectRequest.Builder builder = 
				flowy.scheduler.protocal.Messages.ConnectRequest.newBuilder();
		
		builder.setClientProtocolVersion("FSP_0.1.0");
		return builder.build().toByteArray();
	}

}
