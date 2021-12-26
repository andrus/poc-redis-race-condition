package poc.redis.race.service;

import org.junit.Assert;
import org.junit.Before;

import static org.junit.Assert.fail;
import org.junit.Test;

public class RedisBasicTest {

	@Before
	public void configSLF4J() {
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
	}

	@Test
	public void sanityCheck() {

		final RedisTestWrapper rtw = new RedisTestWrapper();
		rtw.wrap(() -> {
			JedisCache cache = rtw.getCache();
			cache.setScore("A", (short) 0);
			cache.setScore("B", (short) 1);
			Assert.assertEquals(0L, (long) cache.getScore("A"));
			Assert.assertEquals(1L, (long) cache.getScore("B"));
			cache.setScore("A", (short) 2);
			cache.setScore("B", (short) 3);
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				fail("InterruptedException is not supposed to raise here");
			}
			Assert.assertEquals(2L, (long) cache.getScore("A"));
			Assert.assertEquals(3L, (long) cache.getScore("B"));
			cache.setExpirableScore("A", (short) 4, 250);
			cache.setExpirableScore("B", (short) 5, 250);
			Assert.assertEquals(4L, (long) cache.getScore("A"));
			Assert.assertEquals(5L, (long) cache.getScore("B"));
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				fail("InterruptedException is not supposed to raise here");
			}
			Assert.assertNull(cache.getScore("A"));
			Assert.assertNull(cache.getScore("B"));
		});
		Assert.assertFalse(rtw.isRunning());
	}

}
