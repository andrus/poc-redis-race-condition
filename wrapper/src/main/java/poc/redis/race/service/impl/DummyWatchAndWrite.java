package poc.redis.race.service.impl;

import poc.redis.race.service.JedisCache;
import redis.clients.jedis.Jedis;

public class DummyWatchAndWrite extends JedisCache {

	public DummyWatchAndWrite(String host, int port, int ttl, boolean enablePoolTests) {
		super(host, port, ttl, enablePoolTests);
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
		jedis.watch(key); // Nah, watch without transactional mechanics won't do a thing
		return jedis.set(key, value).equals(OK);
	}

	@Override
	public boolean setExpirableValue(Jedis jedis, String key, String value, int forcedTTL) {
		if (value == null) {
			value = "";
		}
		jedis.watch(key); // Nah, watch without transactional mechanics won't do a thing
		if (jedis.set(key, value).equals(OK)) {
			return jedis.pexpire(key, forcedTTL) == 1L;
		} else {
			return false;
		}
	}
	
}
