package poc.redis.race.service;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import org.junit.AfterClass;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poc.redis.race.service.JedisCache.ImplementationVariant;
import redis.clients.jedis.Jedis;

public class RedisRaceTest {

	private static RedisContainer redisContainer;
	private static JedisWrapper jedisWrapper;
	
	private HashMap<String, Integer> kindaDatabase;
	private final String key = "key";
	private final Logger refereeLogger = LoggerFactory.getLogger("🐶 [referee]️");

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
		kindaDatabase = new HashMap<String, Integer>();
		kindaDatabase.put("key", 0);
	}

	@Test
	public void blindWriteTest() throws InterruptedException {
		testCase(ImplementationVariant.BLIND_WRITE, false, false);
	}

	@Test
	public void dummyWatchAndWrite() throws InterruptedException {
		testCase(ImplementationVariant.DUMMY_WATCH_AND_WRITE, false, false);
	}

	@Test
	public void watchTransactionWrite() throws InterruptedException {
		testCase(ImplementationVariant.WATCH_TRANSACTION_WRITE, false, false);
	}

	@Test
	public void checkAndSet() throws InterruptedException {
		testCase(ImplementationVariant.CHECK_AND_SET, true, true);
	}

	@Test
	public void pessimisticLock() throws InterruptedException {
		testCase(ImplementationVariant.PESSIMISTIC_LOCK, true, true);
	}

	@Test
	public void lateCheckAndSet() throws InterruptedException {
		testCase(ImplementationVariant.LATE_CHECK_AND_SET, true, false);
	}

	@Test
	public void latePessimisticLock() throws InterruptedException {
		testCase(ImplementationVariant.LATE_PESSIMISTIC_LOCK, true, false);
	}

	@Test
	public void checkAndSetWithConnectionPerRequest() throws InterruptedException {
		testCase(ImplementationVariant.CHECK_AND_SET, false, false);
	}

	@Test
	public void pessimisticLockWithConnectionPerRequest() throws InterruptedException {
		testCase(ImplementationVariant.PESSIMISTIC_LOCK, false, false);
	}

	private void testCase(ImplementationVariant variant, boolean connectionPerClient, boolean noOverwrite) throws InterruptedException {
		jedisWrapper.setVariant(variant);
		Thread A = new Thread(() -> {
			try {
				mimicService(jedisWrapper, true, connectionPerClient);
			} catch (InterruptedException ex) {
				Assert.fail("InterruptedException is no go for our purpose");
			}
		}, "srvA");
		Thread B = new Thread(() -> {
			try {
				mimicService(jedisWrapper, false, connectionPerClient);
			} catch (InterruptedException ex) {
				Assert.fail("InterruptedException is no go for our purpose");
			}
		}, "srvB");
		A.start();
		B.start();
		refereeLogger.info("Waiting for A to read old value...");
		newValueCanBeWritten.acquire();
		kindaDatabase.put(key, kindaDatabase.get(key) + 1);
		refereeLogger.info("Updated value in Database");
		oldValueRead.release();

		A.join();
		B.join();
		Thread.sleep(50);
		Integer redisFinalValue = jedisWrapper.getCache().getInt(key);
		Integer redisDbValue = kindaDatabase.get(key);
		Assert.assertEquals(Integer.valueOf(1), redisDbValue);
		refereeLogger.info("Final value in Redis: " + redisFinalValue);
		refereeLogger.info("Final value in DB: " + redisDbValue);
		if (noOverwrite) {
			Assert.assertEquals(redisFinalValue, redisDbValue);
		} else {
			Assert.assertNotEquals(redisFinalValue, redisDbValue);
		}
	}

	Semaphore oldValueRead = new Semaphore(0);
	Semaphore newValueCanBeWritten = new Semaphore(0);
	Semaphore newValueWritten = new Semaphore(0);

	private void mimicService(JedisWrapper rtw, boolean saboteur, boolean useSingleConnection) throws InterruptedException {

		final Logger log = LoggerFactory.getLogger((saboteur ? "🐭" : "🐱") + " [service]️");
		JedisCache cache = rtw.getCache();
		log.info("START");
		if (useSingleConnection) {
			try (Jedis jedis = cache.allocJedis()) {
				Integer value = cache.getInt(jedis, key);
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

				boolean valueWasSet = cache.setInt(jedis, key, value);
				if (valueWasSet) {
					log.info("Put value to cache");
				} else {
					log.info("Redis rejected new value");
				}
				if (!saboteur) {
					newValueWritten.release();
				}

				Integer redisValue = cache.getInt(jedis, key);
				log.info("Asked cache for key - got '" + redisValue + "'");
				Thread.sleep(50);
				redisValue = cache.getInt(jedis, key);
				log.info("Asked cache for key - got '" + redisValue + "'");
			} catch (Exception ex) {
				throw ex;
			}
		} else {
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

			boolean valueWasSet = cache.setInt(key, value);
			if (valueWasSet) {
				log.info("Put value to cache");
			} else {
				log.info("Redis rejected new value");
			}

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

}
