package flowy.scheduler.javasdk;

public class Task {
	// task client id
	private String id;
	
	// task execute time in cron expression;
	private String executeTime;
	
	public Task(String id, String executeTime){
		this.id = id;
		this.executeTime = executeTime;
	}
	
	public String getId(){
		return id;
	}
	
	public String getExecuteTime(){
		return executeTime;
	}
}
