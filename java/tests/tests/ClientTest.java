package tests;

import java.io.IOException;
import java.util.Date;

import flowy.scheduler.javasdk.Client;
import flowy.scheduler.javasdk.ITaskNotifyCallback;
import flowy.scheduler.javasdk.Task;

public class ClientTest implements ITaskNotifyCallback {

	public static void main(String[] args) throws InterruptedException {

		ClientTest test = new ClientTest();
		String app_key = "abc";
		String app_secret = "123";

		Client client = new Client("127.0.0.1:3092", app_key, app_secret);
		
		try {
			client.connect();
			Task task = new Task("TestTask", "*/5 * * * * ?");
			client.registerTask(task, test);
			client.unregisterTask(task);
			client.waitTillClose();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ClientTest() {

	}

	@Override
	public void onTaskNotify(Client client, Task task, String instanceId) {
		System.out.println("OnNotify, taskname : " + task.getId() + 
				", instanceid : " + instanceId + 
				new Date());
		client.taskStart(instanceId);
		try {
			Thread.sleep(10000l);
			client.taskRunning(instanceId, 0);
			Thread.sleep(10000l);
			client.taskComplete(instanceId);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
