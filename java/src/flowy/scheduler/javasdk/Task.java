package flowy.scheduler.javasdk;

public class Task {
	private String m_id;
	
	private String m_worker_id;
	
	public Task(String id, String worker_id){
		m_id = id;
		m_worker_id = worker_id;
	}
	
	public String getId(){
		return m_id;
	}
	
	public String getWorkerId(){
		return m_worker_id;
	}
}
