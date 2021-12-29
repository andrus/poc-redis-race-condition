package poc.redis.race.service.impl;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class LateCheckAndSet extends BlindWrite {

	public LateCheckAndSet(String host, int port, int ttl, boolean enablePoolTests) {
		super(host, port, ttl, enablePoolTests);
	}

	@Override
	public boolean _setValue(Jedis jedis, String key, String value) {
		jedis.watch(key);
		Transaction transaction = jedis.multi();
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
		if (value == null) {
			value = "";
		}
		jedis.watch(key);
		Transaction transaction = jedis.multi();
		transaction.set(key, value);
		transaction.pexpire(key, forcedTTL);
		List<Object> exec = transaction.exec();
		if (exec == null) {
			return false;
		}
		for (Object o : exec) {
			if (o == null || (!o.toString().equals("OK") && !o.toString().equals("1"))) {
				return false;
			}
		}
		return true;
	}

}
