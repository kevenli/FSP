import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import flowy.scheduler.entities.Application;

public class DataTest {
	public static void main(String[] args) {
		try {
			SessionFactory sf = new Configuration().configure()
					.buildSessionFactory();
			Session session = sf.openSession();
			Transaction tx = session.beginTransaction();

			for (int i = 0; i < 200; i++) {
				Application application = new Application();
				application.setName("Test");
				application.setAppKey("123");
				application.setAppSecret("321");

				session.save(application);
			}

			tx.commit();
			session.close();
		} catch (HibernateException e) {
			e.printStackTrace();
		}

	}
}
