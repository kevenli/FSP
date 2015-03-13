package flowy.scheduler.javasdk;

public interface ITaskNotifyCallback {

	void onTaskNotify(String taskName, Client client);
}
