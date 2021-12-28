package poc.redis.race.service;

import java.util.HashMap;
import org.junit.AfterClass;
import org.junit.Assert;

import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class RedisBasicTest {

	private static RedisContainer redisContainer;
	private static JedisWrapper jedisWrapper;

	@Rule 
	public TestName name = new TestName();
	
	@BeforeClass
	public static void setup() throws InterruptedException {
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        redisContainer = new RedisContainer();
		redisContainer.start();
		jedisWrapper = new JedisWrapper(redisContainer);
		Thread.sleep(100);
    }
	
	@AfterClass
	public static void tearDown() { 
		jedisWrapper.shutdown();
		redisContainer.stop();
	}
	
	@Before
	public void setupTest() {
		System.out.println();
		System.out.println(TestNameUtils.testName(name));
		jedisWrapper.flushAll();
	}

	@Test
	public void sanityCheck() {
		JedisCache cache = jedisWrapper.getCache();
		cache.setInt("A", 0);
		cache.setInt("B", 1);
		Assert.assertEquals(2, cache.keys().size());
		Assert.assertEquals(Integer.valueOf(0), cache.getInt("A"));
		Assert.assertEquals(Integer.valueOf(1), cache.getInt("B"));
		cache.setInt("A", 2);
		cache.setInt("B", 3);
		try {
			Thread.sleep(JedisCache.DEFAULT_REDIS_TTL_MSEC + 50);
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
			Thread.sleep(JedisCache.DEFAULT_REDIS_TTL_MSEC + 50);
		} catch (InterruptedException e) {
			fail("InterruptedException is not supposed to raise here");
		}
		Assert.assertNull(cache.getValue("A"));
		Assert.assertNull(cache.getValue("B"));
	}
	
	@Test
	public void dbEmitTest() {
		HashMap <String, Integer> kindaDatabase = new HashMap<String, Integer>(){{
			put("key", 0);
		}};
		String key = "key";
		JedisCache cache = jedisWrapper.getCache();
		Assert.assertNull(cache.getInt(key));
		Integer value = kindaDatabase.get(key);
		Assert.assertEquals(Integer.valueOf(0), value);
		Assert.assertTrue(cache.setInt(key, value));
		Assert.assertEquals(Integer.valueOf(0), cache.getInt(key));
	}

}
