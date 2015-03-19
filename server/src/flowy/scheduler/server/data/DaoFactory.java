package flowy.scheduler.server.data;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DaoFactory {

	private static SessionFactory sessionFactory;

	private static Logger logger = Logger.getLogger(DaoFactory.class);

	public static boolean testDatabaseConnection() {
		try {
			if (sessionFactory == null) {
				@SuppressWarnings("deprecation")
				SessionFactory sf = new Configuration().configure()
						.buildSessionFactory();
				sessionFactory = sf;
			}
			
			//org.hibernate.Session session = sessionFactory.openSession();
			//ManagedSessionContext.bind(session);
			
		} catch (Exception ex) {
			logger.error(ex);
			return false;
		}
		return true;
	}
}
