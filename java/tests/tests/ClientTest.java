package tests;

import java.io.IOException;

import flowy.scheduler.javasdk.Client;
import flowy.scheduler.javasdk.IClientCallback;
import flowy.scheduler.javasdk.ITaskNotifyCallback;
import flowy.scheduler.javasdk.Task;
import flowy.scheduler.javasdk.WorkerSetting;

public class ClientTest implements IClientCallback, ITaskNotifyCallback {

	public static void main(String[] args) throws InterruptedException {

		ClientTest test = new ClientTest();
		String app_key = "abc";
		String app_secret = "123";

		WorkerSetting setting = new WorkerSetting("Test_Worker", "≤‚ ‘",
				"*/5 * * * * ?", 30);

		Client client = new Client("127.0.0.1:3092", app_key, app_secret);
		
		try {
			client.connect();
			client.registerTask(new Task("TestTask", "*/5 * * * * ?"), test);
			client.start();
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
	public void OnNotify(Client client, Task task) {
		System.out.println("OnNotify " + task.getId());

	}

	@Override
	public void onTaskNotify(String taskName, Client client) {
		System.out.println("OnNotify, taskname : " + taskName);
	}

}
