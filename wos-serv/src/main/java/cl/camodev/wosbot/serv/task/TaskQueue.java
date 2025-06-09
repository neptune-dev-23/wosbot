package cl.camodev.wosbot.serv.task;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ex.HomeNotFoundException;
import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;

public class TaskQueue {
	// Cola que contendrá todas las tareas (no necesariamente ordenadas por tiempo).
	private final PriorityBlockingQueue<DelayedTask> taskQueue = new PriorityBlockingQueue<>(11, new Comparator<DelayedTask>() {
		@Override
		public int compare(DelayedTask t1, DelayedTask t2) {
			// Si t1 es InitializeTask y t2 no lo es, t1 tiene mayor prioridad.
			if (t1 instanceof InitializeTask && !(t2 instanceof InitializeTask)) {
				return -1;
			} else if (!(t1 instanceof InitializeTask) && t2 instanceof InitializeTask) {
				return 1;
			}
			// Si ambos son (o ninguno), se ordena según el delay.
			return Long.compare(t1.getDelay(TimeUnit.SECONDS), t2.getDelay(TimeUnit.SECONDS));
		}
	});

	// Bandera para detener el loop del scheduler.
	private volatile boolean running = false;

	// Hilo que se encargará de evaluar y ejecutar las tareas.
	private Thread schedulerThread;

	private DTOProfiles profile;

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
			ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Getting queue slot"));
			try {
				EmulatorManager.getInstance().adquireEmulatorSlot(profile.getId(), (thread, position) -> {
					ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Waiting for slot, position: " + position));
				});
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			while (running) {
				boolean executedTask = false;
				long minDelay = Long.MAX_VALUE;

				// realizar preverificacion de que el jeugo esta coriendo

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
						} catch (HomeNotFoundException e) {
							ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, task.getTaskName(), profile.getName(), e.getMessage());
							addTask(new InitializeTask(profile, TpDailyTaskEnum.INITIALIZE));
						} catch (Exception e) {
							ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, task.getTaskName(), profile.getName(), e.getMessage());
						}

						if (task.isRecurring()) {
							ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Next schedule: " + UtilTime.localDateTimeToDDHHMMSS(task.getScheduled()));
							addTask(task);
						} else {
							ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Task removed from schedule");
						}

						executedTask = true;
						break;
					}
				}

				// Verificar condiciones según el delay mínimo de la cola de tareas
				if (minDelay != Long.MAX_VALUE) { // Asegurar que hay tareas en la cola

					long maxIdle = Optional.ofNullable(profile.getGlobalsettings().get(EnumConfigurationKey.MAX_IDLE_TIME_INT.name())).map(Integer::parseInt).orElse(Integer.parseInt(EnumConfigurationKey.MAX_IDLE_TIME_INT.getDefaultValue()));
					; // 30 minutos en segundos
					if (!moreThan30Minutes && minDelay > TimeUnit.MINUTES.toSeconds(maxIdle)) {
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
						String formattedTime;
						if (minDelay == Long.MAX_VALUE || minDelay > 86399) {
							// Si no hay tareas o el delay es muy largo, mostrar un mensaje apropiado
							formattedTime = "No tasks";
						} else {
							DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
							// Convertir minDelay (segundos) a formato HH:mm:ss
							formattedTime = LocalTime.ofSecondOfDay(minDelay).format(timeFormatter);
						}

						ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Idling for " + formattedTime));
						Thread.sleep(999);
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
		EmulatorManager.getInstance().closeEmulator(profile.getEmulatorNumber());
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueue", profile.getName(), "Closing game due to large inactivity");
		LocalDateTime scheduledTime = LocalDateTime.now().plusSeconds(minDelay);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Idling till " + formatter.format(scheduledTime)));
		EmulatorManager.getInstance().releaseEmulatorSlot();
	}

	private void encolarNuevaTarea() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueue", profile.getName(), "shcheduled task's will start soon");

		try {
			EmulatorManager.getInstance().adquireEmulatorSlot(profile.getId(), (thread, position) -> {
				ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "Waiting for slot, position: " + position));
			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		addTask(new InitializeTask(profile, TpDailyTaskEnum.INITIALIZE));
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

}
