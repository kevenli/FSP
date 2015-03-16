package tests;

import java.io.IOException;

import flowy.scheduler.javasdk.Client;
import flowy.scheduler.javasdk.IClientCallback;
import flowy.scheduler.javasdk.Task;
import flowy.scheduler.javasdk.WorkerSetting;

public class ClientTest implements IClientCallback {

	public static void main(String[] args) throws InterruptedException {

		ClientTest test = new ClientTest();
		String app_key = "abc";
		String app_secret = "123";

		WorkerSetting setting = new WorkerSetting("Test_Worker", "≤‚ ‘",
				"*/5 * * * * ?", 30);

		Client client = new Client("127.0.0.1:3092", app_key, app_secret, setting, test);
		
		try {
			client.connect();
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
		// TODO Auto-generated method stub
		System.out.println("OnNotify");

		try {
			client.sendTaskStart(task);

			Thread.sleep(2000);
			client.sendTaskRunning(task);
			Thread.sleep(2000);
			client.sendTaskRunning(task);
			Thread.sleep(2000);
			client.sendTaskComplete(task);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
