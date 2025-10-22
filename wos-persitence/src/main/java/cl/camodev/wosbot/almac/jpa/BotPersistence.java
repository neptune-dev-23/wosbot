package cl.camodev.wosbot.almac.jpa;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

public final class BotPersistence {
    private static final ReentrantLock lock = new ReentrantLock(true);
	private static final String PERSISTENCE_UNIT_NAME = "botPU";
    private static volatile BotPersistence instance;
	private static EntityManagerFactory entityManagerFactory;

	private BotPersistence() {
		try {
			entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
			PersistenceDataInitialization.initializeData();
		} catch (Exception ex) {
			System.err.println("Error inicializando EntityManagerFactory: " + ex.getMessage());
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static BotPersistence getInstance() {
		if (instance == null) {
			synchronized (BotPersistence.class) {
				if (instance == null) {
					instance = new BotPersistence();
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
			entityManager.close(); // Cierra el EntityManager después de cada transacción
            lock.unlock();
        }
	}

	public boolean updateEntity(Object entity) {
		EntityManager entityManager = getEntityManager();
		try {
            lock.lock();
			entityManager.getTransaction().begin();
			entityManager.merge(entity);
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

	public boolean deleteEntity(Object entity) {
		EntityManager entityManager = getEntityManager();
		try {
            lock.lock();
			entityManager.getTransaction().begin();
			entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
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

	public <T> T findEntityById(Class<T> entityClass, Object id) {
		EntityManager entityManager = getEntityManager();
		try {
			return entityManager.find(entityClass, id);
		} finally {
			entityManager.close();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> getQueryResults(String queryString, Class<T> resultClass, Map<String, Object> parameters) {
		EntityManager entityManager = getEntityManager();
		try {
			Query query = entityManager.createQuery(queryString, resultClass);

			// Agregar los parámetros a la Query
			if (parameters != null) {
				for (Map.Entry<String, Object> param : parameters.entrySet()) {
					Object value = param.getValue();
					if (value instanceof String) {
						try {
							value = LocalDateTime.parse((String) value);
						} catch (DateTimeParseException e) {
							// No es un LocalDateTime, se deja el valor como está
						}
					}
					query.setParameter(param.getKey(), value);
				}
			}

			return query.getResultList();
		} finally {
			entityManager.close(); // Cerrar el EntityManager después de la ejecución
		}
	}

    public int executeUpdate(String queryString, Map<String, Object> parameters) {
        EntityManager entityManager = getEntityManager();
        Query query = entityManager.createQuery(queryString);

        if (parameters != null) {
            for (Map.Entry<String, Object> param : parameters.entrySet()) {
                Object value = param.getValue();
                if (value instanceof String) {
                    try {
                        value = LocalDateTime.parse((String) value);
                    } catch (DateTimeParseException e) {
                        // No es un LocalDateTime, se deja el valor como está
                    }
                }
                query.setParameter(param.getKey(), value);
            }
        }

        try {
            lock.lock();
            entityManager.getTransaction().begin();
            int result = query.executeUpdate();
            entityManager.getTransaction().commit();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return 0;
        } finally {
            entityManager.close();
            lock.unlock();
        }
    }

	public void close() {
		if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
			entityManagerFactory.close();
		}
	}
}
