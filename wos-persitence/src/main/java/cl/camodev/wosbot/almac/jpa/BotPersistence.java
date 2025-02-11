package cl.camodev.wosbot.almac.jpa;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

public final class BotPersistence {
	private final String PERSISTENCE_UNIT_NAME = "botPU";
	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;

	private static BotPersistence instance;

	private BotPersistence() {
		try {

			entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
			entityManager = entityManagerFactory.createEntityManager();
		} catch (Exception ex) {
			System.err.println("Error inicializando EntityManagerFactory: " + ex.getMessage());
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static BotPersistence getInstance() {
		if (instance == null) {
			instance = new BotPersistence();
		}
		return instance;
	}

	public boolean createEntity(Object entity) {
		try {
			if (entity != null) {
				entityManager.getTransaction().begin();
				entityManager.persist(entity);
				entityManager.getTransaction().commit();
				return true; // Éxito
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return false;
		}
		return false;
	}

	public boolean updateEntity(Object entity) {
		try {
			if (entity != null) {
				entityManager.getTransaction().begin();
				entityManager.merge(entity);
				entityManager.getTransaction().commit();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return false;
		}
		return false; // Si la entidad es null, retorna false
	}

	public boolean deleteEntity(Object entity) {
		try {
			if (entity != null) {
				entityManager.getTransaction().begin();
				entityManager.remove(entity);
				entityManager.getTransaction().commit();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			return false;
		}
		return false;
	}

	public <T> T findEntityById(Class<T> entityClass, Object id) {
		try {
			return entityManager.find(entityClass, id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public <T> List<T> getQueryResults(Query query) {
		List<T> result = new LinkedList<>();
		Iterator<T> iterator = query.getResultList().iterator();
		while (iterator.hasNext()) {
			result.add(iterator.next());
		}
		return result;
	}

	public Query createNamedQuery(String query) {
		return entityManager.createNamedQuery(query);
	}

	public Query createQuery(String query) {
		return entityManager.createQuery(query);
	}

	/**
	 * Cierra el EntityManagerFactory cuando la aplicación termina.
	 */
	public void close() {
		if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}
}