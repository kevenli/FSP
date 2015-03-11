package flowy.scheduler.server;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import flowy.scheduler.entities.Worker;

public class WorkerDAO {
	private SessionFactory m_session_factory;

	private Session openSession() {
		if (m_session_factory == null) {
			m_session_factory = new AnnotationConfiguration().configure()
					.buildSessionFactory();
		}

		Session session = m_session_factory.openSession();
		return session;
	}

	public Worker createOrUpdate(Worker worker) {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		try {
			Criteria criteria = session.createCriteria(Worker.class);

			criteria.add(Restrictions.eq("applicationId",
					worker.getApplicationId()));
			criteria.add(Restrictions.eq("clientWorkerId",
					worker.getClientWorkerId()));

			Worker exists = (Worker) criteria.uniqueResult();

			if (exists != null) {
				worker.setId(exists.getId());
				exists.setApplicationId(worker.getApplicationId());
				exists.setClientWorkerId(worker.getClientWorkerId());
				exists.setName(worker.getName());
				exists.setSchedule(worker.getSchedule());
				exists.setStatus(worker.getStatus());
				exists.setTimeout(worker.getTimeout());
				exists.setUpdateTime(worker.getUpdateTime());
				session.update(exists);
				tx.commit();
				session.flush();
				return exists;
			}

			session.save(worker);
			tx.commit();
			session.flush();
			return worker;
		} finally {
			session.close();
		}
	}
	
	public Worker updateWorker(Worker worker){
		Session session = openSession();
		Transaction trans = session.beginTransaction();
		
		session.update(worker);
		
		trans.commit();
		
		session.close();
		return worker;
	}
}
