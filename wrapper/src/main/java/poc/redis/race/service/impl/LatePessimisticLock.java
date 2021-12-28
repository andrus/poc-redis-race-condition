package poc.redis.race.service.impl;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class LatePessimisticLock extends BlindWrite {

	private static final String PESSIMISTIC_LOCK_KEY = "PESSIMISTIC_LOCKED";

	public LatePessimisticLock(String host, int port, int ttl, boolean enablePoolTests) {
		super(host, port, ttl, enablePoolTests);
	}

	@Override
	public boolean _setValue(Jedis jedis, String key, String value) {
		jedis.watch(PESSIMISTIC_LOCK_KEY);
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
	public boolean _setExpirableValue(Jedis jedis, String key, String value, int forcedTTL) {
		jedis.watch(PESSIMISTIC_LOCK_KEY);
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
