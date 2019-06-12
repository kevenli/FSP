package flowy.scheduler.server.data;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import flowy.scheduler.server.exceptions.DaoFactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class DaoFactory {

	private static SessionFactory sessionFactory;

	private static Logger logger = LoggerFactory.getLogger(DaoFactory.class);

	public static void init(String host, String username, String password, String dbname, int port) {
		logger.debug("Init database.");
		
		if (sessionFactory == null) {
			
			Configuration configuration = new Configuration();
			configuration.configure();
			String url = String.format("jdbc:mysql://%s:%d/%s?characterEncoding=utf8", host, port, dbname);
			configuration.setProperty("hibernate.connection.url", url);
			configuration.setProperty("hibernate.connection.username", username);
	        configuration.setProperty("hibernate.connection.password", password);
	        
	        
	        ServiceRegistry serviceRegistry = 
	        		new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry(); 
	        
	        SessionFactory sf = 
	        		configuration.buildSessionFactory(serviceRegistry);
			sessionFactory = sf;
			Session session = sf.openSession();
			session.close();
		}
	}
	
	public static SessionFactory getSessionFactory() throws DaoFactoryException{
		if (sessionFactory==null){
			throw new DaoFactoryException("DaoFactory should be initialized first.");
		}
		return sessionFactory;
	}
}
