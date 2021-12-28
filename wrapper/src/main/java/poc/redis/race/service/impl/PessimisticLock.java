package poc.redis.race.service.impl;

import java.util.List;
import poc.redis.race.service.JedisCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class PessimisticLock extends JedisCache {

	private static final String PESSIMISTIC_LOCK_KEY = "PESSIMISTIC_LOCKED";

	public PessimisticLock(String host, int port, int ttl, boolean enablePoolTests) {
		super(host, port, ttl, enablePoolTests);
	}

	@Override
	public String getValue(Jedis jedis, String key) {
		jedis.watch(PESSIMISTIC_LOCK_KEY);
		String value = jedis.get(key);
		if (value != null) {
			return value;
		}
		return null;
	}

	@Override
	public boolean setValue(Jedis jedis, String key, String value) {
		if (value == null) {
			value = "";
		}
		Transaction transaction = jedis.multi();
		transaction.get(PESSIMISTIC_LOCK_KEY);
		transaction.set(PESSIMISTIC_LOCK_KEY, String.valueOf(System.currentTimeMillis()) + "." + String.valueOf(System.nanoTime()));
		transaction.set(key, value);
		List<Object> exec = transaction.exec();
		if (exec == null) {
			return false;
		}
		for (Object o : exec) {
			if (o == null || !o.toString().equals("OK")) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean setExpirableValue(Jedis jedis, String key, String value, int forcedTTL) {
		if (value == null) {
			value = "";
		}
		Transaction transaction = jedis.multi();
		transaction.get(key);
		transaction.set(PESSIMISTIC_LOCK_KEY, String.valueOf(System.currentTimeMillis()) + "." + String.valueOf(System.nanoTime()));
		transaction.set(key, value);
		transaction.pexpire(key, forcedTTL);
		List<Object> exec = transaction.exec();
		if (exec == null) {
			return false;
		}
		for (Object o : exec) {
			if (o == null || !o.toString().equals("OK")) {
				return false;
			}
		}
		return true;
	}
}
