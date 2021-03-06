package flowy.scheduler.server;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DaoFactory {

	private static SessionFactory sessionFactory;

	private static Logger logger = Logger.getLogger(DaoFactory.class);

	public static boolean testDatabaseConnection() {
		try {
			if (sessionFactory == null) {
				SessionFactory sf = new Configuration().configure()
						.buildSessionFactory();
				sessionFactory = sf;
			}

			org.hibernate.Session session = sessionFactory.openSession();
			session.close();
		} catch (Exception ex) {
			logger.error(ex);
			return false;
		}
		return true;

	}
}
