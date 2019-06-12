package flowy.scheduler.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="task_types")
public class TaskType {
	private int id;
	
	private String name;

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId(){
		return this.id;
	}
	
	public void setId(int value){
		this.id = value;
	}
	
	@Column(name = "name")
	public String getName(){
		return this.name;
	}
	
	public void setName(String name){
		this.name = name;
	}
}
