package flowy.scheduler.server.data;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import flowy.scheduler.server.exceptions.DaoFactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@SuppressWarnings("deprecation")
public class DaoFactory {

	private static SessionFactory sessionFactory;

	private static Logger logger = LoggerFactory.getLogger(DaoFactory.class);

	public static void init(Properties config) throws Exception {
		String dbType = config.getProperty("db.type");
		String dbHost = config.getProperty("db.host");
		String dbUsername = config.getProperty("db.username");
		String dbPassword = config.getProperty("db.password");
		String dbName = config.getProperty("db.name");
		logger.debug("Init database.");
		
		if (sessionFactory == null) {
			Configuration configuration = new Configuration();
			configuration.configure();
			dbType = dbType.toLowerCase();
			if(dbType.equals("mysql")){
				int db_port =  Integer.parseInt(config.getProperty("db.port", "3306"));
				String url = String.format("jdbc:mysql://%s:%s/%s?characterEncoding=utf8", dbHost, db_port, dbName);
				configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
				configuration.setProperty("hibernate.connection.driver_class", "org.hibernate.dialect.MySQLDialect");
				configuration.setProperty("hibernate.connection.url", url);
				configuration.setProperty("hibernate.connection.username", dbUsername);
				configuration.setProperty("hibernate.connection.password", dbPassword);
			}
			else if (dbType.equals("sqlite")){
				configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLiteDialect");
				configuration.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
				configuration.setProperty("hibernate.connection.connection_string", "Data Source=" + dbName);
				configuration.setProperty("hibernate.connection.url", "jdbc:sqlite:" + dbName);

			}
			else{
				throw new Exception(String.format("Not supported dbtype : %s", dbType));
			}

	        
	        
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
