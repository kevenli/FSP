package flowy.scheduler.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "workers")
public class Worker {
	private long m_id;
	
	private String m_client_worker_id;
	
	private long m_application_id;
	
	private String m_name;
	
	private String m_schedule;
	
	private int m_timeout;
	
	private Date m_create_time;
	
	private Date m_update_time;
	
	private WorkerStatus m_status;
	
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	public long getId(){
		return m_id;
	}
	
	public void setId(long id){
		m_id = id;
	}
	
	@Column(name = "application_id")
	public long getApplicationId(){
		return m_application_id;
	}
	
	public void setApplicationId(long application_id){
		m_application_id = application_id;
	}

	@Column(name = "client_worker_id")
	public String getClientWorkerId(){
		return m_client_worker_id;
	}
	
	public void setClientWorkerId(String client_worker_id){
		m_client_worker_id = client_worker_id;
	}

	@Column(name = "name")
	public String getName(){
		return m_name;
	}
	
	public void setName(String name){
		m_name = name;
	}

	@Column(name = "schedule")
	public String getSchedule(){
		return m_schedule;
	}
	
	public void setSchedule(String schedule){
		m_schedule = schedule;
	}
	
	@Column(name = "timeout")
	public int getTimeout(){
		return m_timeout;
	}
	
	public void setTimeout(int timeout){
		m_timeout = timeout;
	}
	
	@Column(name = "createtime")
	public Date getCreateTime(){
		return m_create_time;
	}
	
	public void setCreateTime(Date createTime){
		m_create_time = createTime;
	}
	
	@Column(name = "updatetime")
	public Date getUpdateTime(){
		return m_update_time;
	}
	
	public void setUpdateTime(Date updateTime){
		m_update_time = updateTime;
	}
	
	@Enumerated(EnumType.ORDINAL)
	public WorkerStatus getStatus(){
		return m_status;
	}
	
	public void setStatus(WorkerStatus status){
		m_status = status;
	}
}
