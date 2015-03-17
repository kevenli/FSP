package tests;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import flowy.scheduler.javasdk.Client;
import flowy.scheduler.javasdk.Task;
import flowy.scheduler.javasdk.exceptions.TaskAlreadyExistsException;

public class ClientTests {

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
	public void testRegisterTask() throws Exception{
		client.connect();
		Task task = new Task("TestTask", "*/5 * * * * ?");
		client.registerTask(task);
	}
	
	@Test
	public void testRegisterTaskWithException() throws Exception{
		client.connect();
		Task task = new Task("TestTask", "*/5 * * * * ?");
		
		Task task2 = new Task("TestTask", "*/5 * * * * *");
		client.registerTask(task);
		
		try{
			client.registerTask(task2);
			fail("show raise exception here");
		}catch(TaskAlreadyExistsException e){
			
		}
	}

}
