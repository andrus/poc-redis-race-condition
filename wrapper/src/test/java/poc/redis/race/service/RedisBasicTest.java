package poc.redis.race.service;

import java.util.HashMap;
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
			cache.setInt("A", 0);
			cache.setInt("B", 1);
			Assert.assertEquals(2, cache.keys().size());
			Assert.assertEquals(Integer.valueOf(0), cache.getInt("A"));
			Assert.assertEquals(Integer.valueOf(1), cache.getInt("B"));
			cache.setInt("A", 2);
			cache.setInt("B", 3);
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				fail("InterruptedException is not supposed to raise here");
			}
			Assert.assertEquals(Integer.valueOf(2), cache.getInt("A"));
			Assert.assertEquals(Integer.valueOf(3), cache.getInt("B"));
			cache.setExpirableInt("A", 4, 250);
			cache.setExpirableInt("B", 5, 250);
			Assert.assertEquals(Integer.valueOf(4), cache.getInt("A"));
			Assert.assertEquals(Integer.valueOf(5), cache.getInt("B"));
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
				fail("InterruptedException is not supposed to raise here");
			}
			Assert.assertNull(cache.getValue("A"));
			Assert.assertNull(cache.getValue("B"));
		});
		Assert.assertFalse(rtw.isRunning());
	}
	
	@Test
	public void dbEmitTest() {
		HashMap <String, Integer> kindaDatabase = new HashMap<String, Integer>(){{
			put("key", 0);
		}};
		final RedisTestWrapper rtw = new RedisTestWrapper();
		rtw.wrap(() -> {
			String key = "key";
			JedisCache cache = rtw.getCache();
			Assert.assertNull(cache.getInt(key));
			Integer value = kindaDatabase.get(key);
			Assert.assertEquals(Integer.valueOf(0), value);
			Assert.assertTrue(cache.setInt(key, value));
			Assert.assertEquals(Integer.valueOf(0), cache.getInt(key));
		});
		Assert.assertFalse(rtw.isRunning());
	}

}
