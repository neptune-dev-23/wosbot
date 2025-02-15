package cl.camodev.wosbot.serv.task;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;

public class TaskQueue {
	// Cola que contendrá todas las tareas (no necesariamente ordenadas por tiempo).
	private final ConcurrentLinkedQueue<DelayedTask> taskQueue = new ConcurrentLinkedQueue<>();

	// Bandera para detener el loop del scheduler.
	private volatile boolean running = false;

	// Hilo que se encargará de evaluar y ejecutar las tareas.
	private Thread schedulerThread;

	private DTOProfiles profile;

	private InitializeTask initializeTask;

	public TaskQueue(DTOProfiles profile) {
		this.profile = profile;
	}

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

		schedulerThread = new Thread(() -> {
			boolean moreThan30Minutes = false; // Indica si la demora mínima superó los 30 minutos

			while (running) {
				boolean executedTask = false;
				long minDelay = Long.MAX_VALUE; // Para rastrear la tarea con menor delay

				Iterator<DelayedTask> it = taskQueue.iterator();
				while (it.hasNext()) {
					DelayedTask task = it.next();
					long delayInSeconds = task.getDelay(TimeUnit.SECONDS);

					// Obtener el menor delay de todas las tareas en la cola
					if (delayInSeconds < minDelay) {
						minDelay = delayInSeconds;
					}

					// Si la tarea está lista para ejecutarse (delay <= 0)
					if (delayInSeconds <= 0) {
						it.remove();
						try {
							ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Starting task execution");
							ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Executing " + task.getTaskName()));
							task.run();
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (task.isRecurring()) {
							System.out.println("Recurring task, re-qeueing...");
							addTask(task);
						}

						executedTask = true;
						break;
					}
				}

				// Verificar condiciones según el delay mínimo de la cola de tareas
				if (minDelay != Long.MAX_VALUE) { // Asegurar que hay tareas en la cola
					// Si la demora mínima es mayor a 30 minutos y la condición no se ha cumplido aún
					if (!moreThan30Minutes && minDelay > TimeUnit.MINUTES.toSeconds(30)) {
						moreThan30Minutes = true;
						ejecutarFragmentoEspecifico(minDelay);
					}

					// Si la demora baja a menos de 1 minuto y antes se cumplió la condición
					if (moreThan30Minutes && minDelay < TimeUnit.MINUTES.toSeconds(1)) {
						encolarNuevaTarea();
						moreThan30Minutes = false; // Restablecer la condición para futuras evaluaciones
					}
				}

				// Si no se ejecutó ninguna tarea, esperar un poco antes de volver a evaluar
				if (!executedTask) {
					try {
						DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

						// Convertir minDelay (segundos) a formato HH:mm:ss
						String formattedTime = LocalTime.ofSecondOfDay(minDelay).format(timeFormatter);

						ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Idling for " + formattedTime));
						Thread.sleep(300);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		});
		schedulerThread.start();
	}

	// Métodos auxiliares
	private void ejecutarFragmentoEspecifico(long minDelay) {
		EmulatorManager.getInstance().closeGame(initializeTask.getEmulatorNumber());
		EmulatorManager.getInstance().closePlayer(initializeTask.getEmulatorNumber());
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueue", profile.getName(), "Closing game due to large inactivity");
		LocalDateTime scheduledTime = LocalDateTime.now().plusSeconds(minDelay);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Idling till " + formatter.format(scheduledTime)));
	}

	private void encolarNuevaTarea() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueue", profile.getName(), "shcheduled task's will start soon");
		ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "resuming execution"));
		addTask(initializeTask);
	}

	/**
	 * Detiene inmediatamente el procesamiento de la cola, sin importar en qué estado esté.
	 */
	public void stop() {
		running = false; // Detener el bucle principal

		if (schedulerThread != null) {
			schedulerThread.interrupt(); // Interrumpir el hilo para forzar la salida inmediata

			try {
				schedulerThread.join(1000); // Esperar hasta 1 segundo para que el hilo termine
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		// Eliminar todas las tareas pendientes en la cola
		taskQueue.clear();

		System.out.println("TaskQueue detenida de inmediato.");
	}

	public void setInitializeTask(InitializeTask initializeTask) {
		this.initializeTask = initializeTask;
	}
}
