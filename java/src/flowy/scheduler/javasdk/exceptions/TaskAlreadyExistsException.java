package flowy.scheduler.javasdk.exceptions;

public class TaskAlreadyExistsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2279342463566780066L;

	public TaskAlreadyExistsException() {
		super("Task already exists.");
	}
}
