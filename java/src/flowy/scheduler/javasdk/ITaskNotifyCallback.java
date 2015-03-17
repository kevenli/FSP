package flowy.scheduler.javasdk;

public interface ITaskNotifyCallback {

	void onTaskNotify(Client client, Task task, String instanceId);
}
