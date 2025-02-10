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

				Iterator<DelayedTask> it = taskQueue.iterator();
				while (it.hasNext()) {
					DelayedTask task = it.next();
					if (task.getDelay(TimeUnit.SECONDS) <= 0) {
						it.remove();

						try {

							task.run(); 
						} catch (Exception e) {
							e.printStackTrace();
						}

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
