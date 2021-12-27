package poc.redis.race.service;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poc.redis.race.service.JedisCache.ImplementationVariant;
import redis.clients.jedis.Jedis;

public class RedisRaceTest {

	HashMap<String, Integer> kindaDatabase;
	String key = "key";

	@Before
	public void configSLF4J() {
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
	}

	@Test
	public void blindWriteTest() {
		testCase(ImplementationVariant.BLIND_WRITE, false, false);
	}

	@Test
	public void dummyWatchAndWrite() {
		testCase(ImplementationVariant.DUMMY_WATCH_AND_WRITE, false, false);
	}

	@Test
	public void watchTransactionWrite() {
		testCase(ImplementationVariant.WATCH_TRANSACTION_WRITE, false, false);
	}

	@Test
	public void checkAndSet() {
		testCase(ImplementationVariant.CHECK_AND_SET, true, true);
	}
	
	@Test
	public void pessimisticLock() {
		testCase(ImplementationVariant.PESSIMISTIC_LOCK, true, true);
	}
	
	@Test
	public void lateCheckAndSet() {
		testCase(ImplementationVariant.LATE_CHECK_AND_SET, true, false);
	}
	
	@Test
	public void latePessimisticLock() {
		testCase(ImplementationVariant.LATE_PESSIMISTIC_LOCK, true, false);
	}

	@Test
	public void checkAndSetWithConnectionPerRequest() {
		testCase(ImplementationVariant.CHECK_AND_SET, false, false);
	}
	
	@Test
	public void pessimisticLockWithConnectionPerRequest() {
		testCase(ImplementationVariant.PESSIMISTIC_LOCK, false, false);
	}


	private void testCase(ImplementationVariant variant, boolean connectionPerClient, boolean noOverwrite) {
		kindaDatabase = new HashMap<String, Integer>();
		kindaDatabase.put("key", 0);
		final RedisTestWrapper rtw = new RedisTestWrapper(variant);
		rtw.wrap(() -> {
			final org.slf4j.Logger log = LoggerFactory.getLogger("🐶 [referee]️");
			try {
				Thread A = new Thread(() -> {
					try {
						mimicService(rtw, true, connectionPerClient);
					} catch (InterruptedException ex) {
						Assert.fail("InterruptedException is no go for our purpose");
					}
				}, "srvc");
				Thread B = new Thread(() -> {
					try {
						mimicService(rtw, false, connectionPerClient);
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
				Integer redisDbValue = kindaDatabase.get(key);
				Assert.assertEquals(Integer.valueOf(1), redisDbValue);
				log.info("Final value in Redis: " + redisFinalValue);
				log.info("Final value in DB: " + redisDbValue);
				if (noOverwrite) {
					Assert.assertEquals(redisFinalValue, redisDbValue);
				} else {
					Assert.assertNotEquals(redisFinalValue, redisDbValue);
				}
			} catch (InterruptedException ex) {
				Assert.fail("InterruptedException is no go in our case");
			} catch (AssertionError ae) {
				throw ae;
			} catch (Exception e) {
				throw e;
			}

		});
		Assert.assertFalse(rtw.isRunning());
	}

	Semaphore oldValueRead = new Semaphore(0);
	Semaphore newValueCanBeWritten = new Semaphore(0);
	Semaphore newValueWritten = new Semaphore(0);

	private void mimicService(RedisTestWrapper rtw, boolean saboteur, boolean useSingleConnection) throws InterruptedException {

		final Logger log = LoggerFactory.getLogger(saboteur ? "🐭" : "🐱" + " [service]️");
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
