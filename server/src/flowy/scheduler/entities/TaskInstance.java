package flowy.scheduler.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="task_instances")
public class TaskInstance {

	private String id;
	
	private int taskId;
	
	private int sessionId;
	
	private Date fireTime;
	
	private Date updateTime;
	
	private Date completeTime;
	
	private TaskStatus status;
	
	public TaskInstance() {

	}
	
	@Id
	@Column(name = "id")
	public String getId(){
		return this.id;
	}

	public void setId(String id){
		this.id = id;
	}
	
	@Column(name = "task_id")
	public int getTaskId(){
		return this.taskId;
	}
	
	public void setTaskId(int taskId){
		this.taskId = taskId;
	}
	
	@Column(name = "session_id")
	public int getSessionId(){
		return this.sessionId;
	}
	
	public void setSessionId(int sessionId){
		this.sessionId = sessionId;
	}
	
	@Column(name = "fire_time")
	public Date getFireTime(){
		return this.fireTime;
	}
	
	public void setFireTime(Date fireTime){
		this.fireTime = fireTime;
	}
	
	@Column(name = "update_time")
	public Date getUpdateTime(){
		return this.updateTime;
	}
	
	public void setUpdateTime(Date updateTime){
		this.updateTime = updateTime;
	}
	
	@Column(name = "complete_time")
	public Date getCompleteTime(){
		return this.completeTime;
	}
	
	public void setCompleteTime(Date completeTime){
		this.completeTime = completeTime;
	}
	
	@Column(name = "status")
	public TaskStatus getStatus(){
		return this.status;
	}
	
	public void setStatus(TaskStatus status){
		this.status = status;
	}
}
