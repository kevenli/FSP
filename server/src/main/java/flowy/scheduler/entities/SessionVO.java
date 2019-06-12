package flowy.scheduler.entities;

import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;


@Entity(name="sessions")
public class SessionVO {

	private int id;
	
	private int applicationId;
	
	private String clientIp;
	
	private Date createTime;
	
	private Set<Task> tasks;
	
	public SessionVO() {
		
	}
	
	
	public void setId(int id){
		this.id= id;
	}
	@Id
	@Column(name = "id")
	public int getId(){
		return this.id;
	}
	
	
	public void setApplicationId(int applicationId){
		this.applicationId = applicationId;
	}
	
	@Column(name = "application_id")
	public int getApplicationId(){
		return this.applicationId;
	}
	
	public void setClientIp(String clientIp){
		this.clientIp = clientIp;
	}
	
	@Column(name = "client_ip")
	public String getClientIp(){
		return this.clientIp;
	}
	
	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}
	
	@Column(name = "create_time")
	public Date getCreateTime(){
		return this.createTime;
	}
	
	public void setTasks(Set<Task> tasks){
		this.tasks = tasks;
	}
	
	@ManyToMany(fetch = FetchType.EAGER, 
			cascade={CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
	@JoinTable(name = "task_sessions", joinColumns = { 
			@JoinColumn(name = "session_id", nullable = false, updatable = false) }, 
			inverseJoinColumns = { @JoinColumn(name = "task_id", 
					nullable = false, updatable = false) })
	public Set<Task> getTasks(){
		return this.tasks;
	}
	
	

}
