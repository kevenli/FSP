package flowy.scheduler.server.data;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import flowy.scheduler.server.exceptions.DaoFactoryException;

public abstract class DAOBase {

	protected Session openSession() {
		Session session = null;
		try {
			session = DaoFactory.getSessionFactory().openSession();
		} catch (HibernateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DaoFactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return session;
	}

}
