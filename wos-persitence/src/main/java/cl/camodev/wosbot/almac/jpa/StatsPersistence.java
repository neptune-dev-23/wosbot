package cl.camodev.wosbot.almac.jpa;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

public final class StatsPersistence {

    private static final String PERSISTENCE_UNIT_NAME = "statsPU";
    private static StatsPersistence instance;
    private static EntityManagerFactory entityManagerFactory;
    private static final ReentrantLock lock = new ReentrantLock(true);

    private StatsPersistence() {
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        } catch (Exception ex) {
            System.err.println("Error inicializando Stats EntityManagerFactory: " + ex.getMessage());
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static StatsPersistence getInstance() {
        if (instance == null) {
            synchronized (StatsPersistence.class) {
                if (instance == null) {
                    instance = new StatsPersistence();
                }
            }
        }
        return instance;
    }

    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    public boolean createEntity(Object entity) {
        EntityManager entityManager = getEntityManager();
        try {
            lock.lock();
            entityManager.getTransaction().begin();
            entityManager.persist(entity);
            entityManager.getTransaction().commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return false;
        } finally {
            entityManager.close();
            lock.unlock();
        }
    }

    public <T> List<T> getQueryResults(String queryString,
                                       Class<T> resultClass,
                                       Map<String, Object> parameters,
                                       Integer maxResults) {
        EntityManager entityManager = getEntityManager();
        try {
            TypedQuery<T> query = entityManager.createQuery(queryString, resultClass);
            if (parameters != null) {
                parameters.forEach(query::setParameter);
            }
            if (maxResults != null && maxResults > 0) {
                query.setMaxResults(maxResults);
            }
            return query.getResultList();
        } finally {
            entityManager.close();
        }
    }

    public void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }
}
