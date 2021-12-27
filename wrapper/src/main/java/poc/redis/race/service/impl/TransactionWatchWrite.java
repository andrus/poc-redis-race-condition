package poc.redis.race.service.impl;

import java.util.List;
import poc.redis.race.service.JedisCache;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class TransactionWatchWrite extends JedisCache {

	public TransactionWatchWrite(String host, int port, int ttl, String loggerPrefix, boolean enablePoolTests) {
		super(host, port, ttl, loggerPrefix, enablePoolTests);
	}

	@Override
	public String getValue(Jedis jedis, String key) {
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
		transaction.set(key, value);
		transaction.watch(key);
		List<Object> exec = transaction.exec();
		for (Object o : exec) {
			if (!o.toString().equals("OK")) {
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
		transaction.set(key, value);
		transaction.pexpire(key, forcedTTL);
		transaction.watch(key);
		List<Object> exec = transaction.exec();
		for (Object o : exec) {
			if (!o.toString().equals("OK")) {
				return false;
			}
		}
		return true;
	}

}
