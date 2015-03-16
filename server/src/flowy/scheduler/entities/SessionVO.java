package flowy.scheduler.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name="sessions")
public class SessionVO {

	private int id;
	
	private int applicationId;
	
	private String clientIp;
	
	private Date createTime;
	
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

}
