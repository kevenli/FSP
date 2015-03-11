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
@Table(name="tasks")
public class Task {
	private long m_id;
	
	private long m_worker_id;
	
	private Date m_create_time;
	
	private Date m_start_time;
	
	private Date m_complete_time;
	
	private Date m_update_time;
	
	private TaskStatus m_status;
	
	
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public long getId(){
		return m_id;
	}
	
	public void setId(long id){
		m_id = id;
	}

	@Column(name = "worker_id")
	public long getWorkerId(){
		return m_worker_id;
	}
	
	public void setWorkerId(long worker_id){
		m_worker_id = worker_id;
	}

	@Column(name = "create_time")
	public Date getCreateTime(){
		return m_create_time;
	}
	
	public void setCreateTime(Date create_time){
		m_create_time = create_time;
	}
	
	@Column(name = "start_time")
	public Date getStartTime(){
		return m_start_time;
	}
	
	public void setStartTime(Date start_time){
		m_start_time = start_time;
	}
	
	@Column(name = "complete_time")
	public Date getCompleteTime(){
		return m_complete_time;
	}
	
	public void setCompleteTime(Date complete_time){
		m_complete_time = complete_time;
	}
	
	@Column(name = "update_time")
	public Date getUpdateTime(){
		return m_update_time;
	}
	
	public void setUpdateTime(Date update_time){
		m_update_time = update_time;
	}
	
	@Enumerated(EnumType.ORDINAL)
	public TaskStatus getStatus(){
		return m_status;
	}
	
	public void setStatus(TaskStatus status){
		m_status = status;
	}
}
