package flowy.scheduler.javasdk;

public interface ITaskNotifyCallback {

	void onTaskNotify(Task task, Client client);
}
