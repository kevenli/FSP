package flowy.scheduler.server.data;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import flowy.scheduler.entities.Application;

public class ApplicationDAO extends DAOBase {

	public Application getApplication(String appKey) {
		Session session = openSession();
		try {
			Criteria criteria = session.createCriteria(Application.class);
			criteria.add(Restrictions.eq("appKey", appKey));
			Object obj = criteria.uniqueResult();
			if (obj == null) {
				return null;
			}
			Application application = (Application) obj;
			return application;
		}
		finally{
			session.close();
		}
	}
}
