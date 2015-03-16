package flowy.scheduler.server.data;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import flowy.scheduler.entities.Application;

public class ApplicationDAO {
	
	
	public static Application getApplication(String appKey){
		SessionFactory sf = new Configuration().configure()
				.buildSessionFactory();
		Session session = sf.openSession();
		try {
			
			
			Criteria criteria = session.createCriteria(Application.class);
			criteria.add(Restrictions.eq("appKey", appKey));
			Object obj = criteria.uniqueResult();
			if (obj == null){
				return null;
			}
			Application application = (Application)obj ;
			return application;

		} catch (HibernateException e) {
			e.printStackTrace();
			throw e;
		} finally{
			session.close();
		}
	}
}
