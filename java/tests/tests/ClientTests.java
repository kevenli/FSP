package tests;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import flowy.scheduler.javasdk.Client;
import flowy.scheduler.javasdk.ITaskNotifyCallback;
import flowy.scheduler.javasdk.Task;
import flowy.scheduler.javasdk.exceptions.TaskAlreadyExistsException;

public class ClientTests implements ITaskNotifyCallback {

	private Client client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		String app_key = "abc";
		String app_secret = "123";

		client = new Client("127.0.0.1:3092", app_key, app_secret);
	}

	@After
	public void tearDown() throws Exception {
		client.close();
	}

	@Test
	public void testConnect() throws Exception {
		client.connect();
	}

	@Test
	public void testRegisterTask() throws Exception {
		client.connect();
		Task task = new Task("TestTask", "5 * * * * ?");
		client.registerTask(task, this);
	}
	
	@Test
	public void testUnregisterTask() throws Exception {
		client.connect();
		Task task = new Task("TestTask", "*/5 * * * * ?");
		client.registerTask(task, this);
		client.unregisterTask(task);
	}

	@Test
	public void testRegisterTaskWithException() throws Exception {
		client.connect();
		Task task = new Task("TestTask", "*/5 * * * * ?");

		Task task2 = new Task("TestTask", "*/5 * * * * *");
		client.registerTask(task, this);

		try {
			client.registerTask(task2, this);
			fail("should raise exception here");
		} catch (TaskAlreadyExistsException e) {

		}
	}

	@Test
	public void testTaskRun() throws Exception {
		client.connect();
		Task task = new Task("TestTask", "*/5 * * * * ?");
		client.registerTask(task, new ITaskNotifyCallback() {
			@Override
			public void onTaskNotify(Client client, Task task, String instanceId) {
				client.taskStart(instanceId);

				try {
					Thread.sleep(10000l);
					client.taskRunning(instanceId, 10);

					Thread.sleep(10000l);
					client.taskRunning(instanceId, 50);

					Thread.sleep(10000l);
					client.taskComplete(instanceId);
					
					client.close();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		client.waitTillClose();
	}

	@Override
	public void onTaskNotify(Client client, Task task, String instanceId) {
		System.out.println("OnNotify, taskname : " + task.getId());

	}
}
