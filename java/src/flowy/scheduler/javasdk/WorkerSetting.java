package flowy.scheduler.javasdk;

public class WorkerSetting {

	private String m_worker_id;
	
	private String m_worker_name;
	
	private String m_execute_time;
	
	private int m_timeout = 30;
	
	public WorkerSetting(String worker_id, String worker_name, String execute_time, int timeout){
		m_worker_id = worker_id;
		m_worker_name = worker_name;
		m_execute_time = execute_time;
		m_timeout = timeout;
	}
	
	public String getWorkerId(){
		return m_worker_id;
	}
	
	public String getWorkerName(){
		return m_worker_name;
	}
	
	public String getExecuteTime(){
		return m_execute_time;
	}
	
	public int getTimeout(){
		return m_timeout;
	}
}
