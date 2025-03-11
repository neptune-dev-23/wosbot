package cl.camodev.wosbot.serv.task;

public class WaitingThread implements Comparable<WaitingThread> {
	final Thread thread;
	final Long priority;
	final Long arrivalTime;

	public WaitingThread(Thread thread, Long priority) {
		this.thread = thread;
		this.priority = priority;
		this.arrivalTime = System.nanoTime(); // Marca de tiempo para desempatar
	}

	@Override
	public int compareTo(WaitingThread other) {
		// Se ordena de menor a mayor prioridad (valor menor = mayor prioridad)
		int cmp = Long.compare(this.priority, other.priority);
		if (cmp == 0) {
			// Si tienen la misma prioridad, el que llegó primero tiene preferencia.
			cmp = Long.compare(this.arrivalTime, other.arrivalTime);
		}
		return cmp;
	}

}