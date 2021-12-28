package poc.redis.race.service.impl;

import redis.clients.jedis.Jedis;

public class DummyWatchAndWrite extends BlindWrite {

	public DummyWatchAndWrite(String host, int port, int ttl, boolean enablePoolTests) {
		super(host, port, ttl, enablePoolTests);
	}

	@Override
	public boolean _setValue(Jedis jedis, String key, String value) {
		jedis.watch(key); // Nah, watch without transactional mechanics won't do a thing
		return jedis.set(key, value).equals(OK);
	}

	@Override
	public boolean _setExpirableValue(Jedis jedis, String key, String value, int forcedTTL) {
		jedis.watch(key); // Nah, watch without transactional mechanics won't do a thing
		if (jedis.set(key, value).equals(OK)) {
			return jedis.pexpire(key, forcedTTL) == 1L;
		} else {
			return false;
		}
	}
	
}
