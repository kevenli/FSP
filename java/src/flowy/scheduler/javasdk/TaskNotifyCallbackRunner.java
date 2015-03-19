package flowy.scheduler.javasdk;

public class TaskNotifyCallbackRunner implements Runnable  {

	private Client client;
	private Task task;
	private String instanceId;
	private ITaskNotifyCallback callback;
	
	public TaskNotifyCallbackRunner(Client client, 
			Task task, 
			String instanceId, 
			ITaskNotifyCallback callback) {
		this.client = client;
		this.task = task;
		this.instanceId = instanceId;
		this.callback = callback;
	}

	@Override
	public void run() {
		try{
			callback.onTaskNotify(client, task, instanceId);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

}
