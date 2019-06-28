package flowy.scheduler.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="tasks")
public class Task {
	private int id;
	
	private int applicationId;
	
	// task id specified by client
	private String clientTaskId;
	
	// task execute time in cron expression
	private String executeTime;
	
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	public int getId(){
		return id;
	}
	
	public void setId(int id){
		this.id = id;
	}

	@Column(name = "application_id")
	public int getApplicationId(){
		return applicationId;
	}
	
	public void setApplicationId(int applicationId){
		this.applicationId = applicationId;
	}
	
	@Column(name = "client_task_id")
	public String getClientTaskId(){
		return this.clientTaskId;
	}
	
	public void setClientTaskId(String clientTaskId){
		this.clientTaskId = clientTaskId;
	}

	@Column(name = "execute_time")
	public String getExecuteTime(){
		return executeTime;
	}
	
	public void setExecuteTime(String executeTime){
		this.executeTime = executeTime;
	}
	
}
