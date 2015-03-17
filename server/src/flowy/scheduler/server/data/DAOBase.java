package flowy.scheduler.server.data;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public abstract class DAOBase {

	private SessionFactory sessionFactory;
	@SuppressWarnings("deprecation")
	public DAOBase() {
		Configuration cfg = new Configuration();
		cfg.configure();
		sessionFactory = cfg.buildSessionFactory();
	}

	protected Session openSession() {
		return sessionFactory.openSession();
	}

}
