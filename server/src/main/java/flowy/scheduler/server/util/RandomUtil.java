package flowy.scheduler.server.util;

import java.util.Random;

public class RandomUtil {

	private static Random seed = new Random();

	public static int randomPositiveInt(){
		int ret;
		do{
			ret = seed.nextInt();
		}while(ret <= 0);
		return ret;
	}
}
