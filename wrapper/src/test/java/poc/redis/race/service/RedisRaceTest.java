package poc.redis.race.service;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisRaceTest {

	HashMap<String, Integer> kindaDatabase;
	String key = "key";

	@Before
	public void configSLF4J() {
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
	}

	@Test
	public void dbEmitTest() {
		kindaDatabase = new HashMap<String, Integer>();
		kindaDatabase.put("key", 0);
		final RedisTestWrapper rtw = new RedisTestWrapper();
		rtw.wrap(() -> {
			final org.slf4j.Logger log = LoggerFactory.getLogger("🐶 [referee]️");
			try {

				Thread A = new Thread(() -> {
					try {
						mimicService(rtw, true);
					} catch (InterruptedException ex) {
						Assert.fail("InterruptedException is no go for our purpose");
					}
				}, "srvc");
				Thread B = new Thread(() -> {
					try {
						mimicService(rtw, false);
					} catch (InterruptedException ex) {
						Assert.fail("InterruptedException is no go for our purpose");
					}
				}, "srvc");
				rtw.getCache("monitor");
				A.start();
				B.start();
				log.info("Waiting for A to read old value...");
				newValueCanBeWritten.acquire();
				kindaDatabase.put(key, kindaDatabase.get(key) + 1);
				log.info("Updated value in Database");
				oldValueRead.release();

				A.join();
				B.join();
				Thread.sleep(50);
				Integer redisFinalValue = rtw.getCache("final-state-check").getInt(key);
				Assert.assertEquals(Integer.valueOf(0), redisFinalValue);
				Integer redisDbValue = kindaDatabase.get(key);
				Assert.assertEquals(Integer.valueOf(1), redisDbValue);
				log.info("Final value in Redis: " + redisFinalValue);
				log.info("Final value in DB: " + redisDbValue);
				Assert.assertNotEquals(redisFinalValue, redisDbValue);
			} catch (InterruptedException ex) {
				Assert.fail("InterruptedException is no go in our case");
			} catch (AssertionError ae) {
				throw ae;
			}

		});
		Assert.assertFalse(rtw.isRunning());
	}

	Semaphore oldValueRead = new Semaphore(0);
	Semaphore newValueCanBeWritten = new Semaphore(0);
	Semaphore newValueWritten = new Semaphore(0);

	private void mimicService(RedisTestWrapper rtw, boolean saboteur) throws InterruptedException {

		final Logger log = LoggerFactory.getLogger(saboteur ? "🐭" : "🐱" + " [service]️");
		JedisCache cache = rtw.getCache();
		log.info("START");
		Integer value = cache.getInt(key);
		Assert.assertNull(value);
		log.info("Asked cache for key - got '" + value + "'");

		if (!saboteur) {
			oldValueRead.acquire();
		}

		value = kindaDatabase.get(key);
		log.info("Asked db for key    - got '" + value + "'");

		if (saboteur) {
			newValueCanBeWritten.release();
			newValueWritten.acquire();
		}

		Assert.assertTrue(cache.setInt(key, value));
		log.info("Put value to cache");

		if (!saboteur) {
			newValueWritten.release();
		}

		Integer redisValue = cache.getInt(key);
		log.info("Asked cache for key - got '" + redisValue + "'");
		Thread.sleep(50);
		redisValue = cache.getInt(key);
		log.info("Asked cache for key - got '" + redisValue + "'");
	}

}
