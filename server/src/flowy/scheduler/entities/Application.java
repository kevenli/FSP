package flowy.scheduler.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name="applications")
public class Application {
	
	private int m_id;
	
	private String m_name;
	
	private String m_app_key;
	
	private String m_app_secret;
	
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId(){
		return m_id;
	}
	
	public void setId(int id){
		m_id = id;
	}
	
	@Column(name = "name")
	public String getName(){
		return m_name;
	}
	
	public void setName(String name){
		m_name = name;
	}

	@Column(name = "app_key")
	public String getAppKey(){
		return m_app_key;
	}

	public void setAppKey(String appKey){
		m_app_key = appKey;
	}
	
	@Column(name = "app_secret")
	public String getAppSecret(){
		return m_app_secret;
	}

	public void setAppSecret(String appSecret){
		m_app_secret = appSecret;
	}

	public Application(){
		
	}
}
