package cl.camodev.wosbot.serv.task;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class TaskQueue {
	// Cola que contendrá todas las tareas (no necesariamente ordenadas por tiempo).
	private final ConcurrentLinkedQueue<DelayedTask> taskQueue = new ConcurrentLinkedQueue<>();

	// Bandera para detener el loop del scheduler.
	private volatile boolean running = false;

	// Hilo que se encargará de evaluar y ejecutar las tareas.
	private Thread schedulerThread;

	/**
	 * Agrega una tarea a la cola.
	 */
	public void addTask(DelayedTask task) {
		taskQueue.offer(task);
	}

	/**
	 * Inicia el procesamiento de la cola.
	 */
	public void start() {
		if (running)
			return;
		running = true;

		// Creamos y arrancamos el hilo scheduler.
		schedulerThread = new Thread(() -> {
			while (running) {
				boolean executedTask = false;

				// Recorremos la cola mediante un iterador.
				Iterator<DelayedTask> it = taskQueue.iterator();
				while (it.hasNext()) {
//					System.out.println("Iterating task queue");
					DelayedTask task = it.next();
					// Si la tarea está lista para ejecutarse (es decir, su delay es <= 0)
//					System.out.println("Task " + task.taskName + " delay: " + task.getDelay(TimeUnit.MILLISECONDS));
					if (task.getDelay(TimeUnit.MILLISECONDS) <= 0) {
						// La removemos de la cola
						it.remove();

						// Ejecutamos la tarea (aquí se ejecuta completamente en este mismo hilo)
						try {
							System.out.println("Executing task " + task.taskName);
							task.run(); // Dentro se llamará a execute() según la implementación
						} catch (Exception e) {
							e.printStackTrace();
						}

						// Si la tarea es recurrente, se reprograma y se vuelve a agregar a la cola
						if (task.isRecurring()) {
							System.out.println("Rescheduling task");
							addTask(task);
						}

						// Se ejecuta solo una tarea por iteración (única ejecución a la vez)
						executedTask = true;
						break;
					}
				}

				// Si no se ejecutó ninguna tarea, esperamos un poco antes de volver a evaluar
				if (!executedTask) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		});
		schedulerThread.start();
	}

	/**
	 * Detiene el procesamiento de la cola.
	 */
	public void stop() {
		running = false;
		if (schedulerThread != null) {
			schedulerThread.interrupt();
		}
	}
}
