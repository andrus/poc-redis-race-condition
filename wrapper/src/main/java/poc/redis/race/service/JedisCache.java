package poc.redis.race.service;

import java.util.Set;

import poc.redis.race.service.impl.BlindWrite;
import poc.redis.race.service.impl.CheckAndSet;
import poc.redis.race.service.impl.DummyWatchAndWrite;
import poc.redis.race.service.impl.LateCheckAndSet;
import poc.redis.race.service.impl.LatePessimisticLock;
import poc.redis.race.service.impl.PessimisticLock;
import poc.redis.race.service.impl.WatchTransactionWrite;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public abstract class JedisCache {

	public static final int DEFAULT_REDIS_TTL_MSEC = 300;
	public static final String OK = "OK";

	enum ImplementationVariant {
		BLIND_WRITE,
		DUMMY_WATCH_AND_WRITE,
		WATCH_TRANSACTION_WRITE,
		CHECK_AND_SET,
		PESSIMISTIC_LOCK,
		LATE_CHECK_AND_SET,
		LATE_PESSIMISTIC_LOCK
	}

	private final int ttl;
	private JedisPool pool = null;

	protected JedisCache(String host, int port, int ttl, boolean enablePoolTests) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(128);
		config.setMaxIdle(128);
		config.setMinIdle(16);
		config.setBlockWhenExhausted(true);
		if (enablePoolTests) {
			config.setTestOnBorrow(true);
			config.setTestOnReturn(true);
			config.setTestWhileIdle(true);
			config.setNumTestsPerEvictionRun(3);
		}
		pool = new JedisPool(config, host, port);
		this.ttl = ttl;
	}

	public static JedisCache init(String host, int port) {
		return init(host, port, DEFAULT_REDIS_TTL_MSEC);
	}

	public static JedisCache init(String host, int port, int ttl) {
		return init(host, port, ttl, false, ImplementationVariant.BLIND_WRITE);
	}

	public static JedisCache init(String host, int port, int ttl, boolean enablePoolTests, ImplementationVariant variant) {
		switch (variant) {
			case DUMMY_WATCH_AND_WRITE: {
				return new DummyWatchAndWrite(host, port, ttl, enablePoolTests);
			}
			case WATCH_TRANSACTION_WRITE: {
				return new WatchTransactionWrite(host, port, ttl, enablePoolTests);
			}
			case CHECK_AND_SET: {
				return new CheckAndSet(host, port, ttl, enablePoolTests);
			}
			case PESSIMISTIC_LOCK: {
				return new PessimisticLock(host, port, ttl, enablePoolTests);
			}
			case LATE_CHECK_AND_SET: {
				return new LateCheckAndSet(host, port, ttl, enablePoolTests);
			}
			case LATE_PESSIMISTIC_LOCK: {
				return new LatePessimisticLock(host, port, ttl, enablePoolTests);
			}
			default: {
				return new BlindWrite(host, port, ttl, enablePoolTests);
			}
		}
	}

	public Set<String> keys() {
		try (Jedis jedis = allocJedis()) {
			return jedis.keys("*");
		}
	}

	protected Jedis allocJedis() {
		if (pool == null) {
			throw new RuntimeException("JedisCache needs to be init() before any activity");
		}
		return pool.getResource();
	}

	public String getValue(String key) {
		try (Jedis jedis = allocJedis()) {
			return getValue(jedis, key);
		}
	}

	public boolean setValue(String key, String value) {
		try (Jedis jedis = allocJedis()) {
			return setValue(jedis, key, value);
		}
	}

	public boolean setExpirableValue(String key, String value, int forcedTTL) {
		try (Jedis jedis = allocJedis()) {
			return setExpirableValue(jedis, key, value);
		}
	}

	public abstract String getValue(Jedis jedis, String key);

	public abstract boolean setValue(Jedis jedis, String key, String value);

	public abstract boolean setExpirableValue(Jedis jedis, String key, String value, int forcedTTL);

	public Short getShort(String key) {
		String value = getValue(key);
		return value == null ? null : Short.parseShort(value);
	}

	public Integer getInt(String key) {
		String value = getValue(key);
		return value == null ? null : Integer.parseInt(value);
	}

	public Long getLong(String key) {
		String value = getValue(key);
		return value == null ? null : Long.parseLong(value);
	}

	public boolean setShort(String key, Short value) {
		return setValue(key, wrapNull(value, ""));
	}

	public boolean setInt(String key, Integer value) {
		return setValue(key, wrapNull(value, ""));
	}

	public boolean setLong(String key, Long value) {
		return setValue(key, wrapNull(value, ""));
	}

	public boolean setExpirableInt(String key, Integer value) {
		return setExpirableValue(key, wrapNull(value, ""), ttl);
	}

	public boolean setExpirableShort(String key, Short value) {
		return setExpirableValue(key, wrapNull(value, ""), ttl);
	}

	public boolean setExpirableLong(String key, Long value) {
		return setExpirableValue(key, wrapNull(value, ""), ttl);
	}

	public boolean setExpirableValue(String key, String value) {
		return setExpirableValue(key, value, ttl);
	}

	public boolean setExpirableInt(String key, Integer value, int forcedTTL) {
		return setExpirableValue(key, wrapNull(value, ""), forcedTTL);
	}

	public boolean setExpirableLong(String key, Long value, int forcedTTL) {
		return setExpirableValue(key, wrapNull(value, ""), forcedTTL);
	}

	public Short getShort(Jedis jedis, String key) {
		String value = getValue(jedis, key);
		return value == null ? null : Short.parseShort(value);
	}

	public Integer getInt(Jedis jedis, String key) {
		String value = getValue(jedis, key);
		return value == null ? null : Integer.parseInt(value);
	}

	public Long getLong(Jedis jedis, String key) {
		String value = getValue(jedis, key);
		return value == null ? null : Long.parseLong(value);
	}

	public boolean setShort(Jedis jedis, String key, Short value) {
		return setValue(jedis, key, wrapNull(value, ""));
	}

	public boolean setInt(Jedis jedis, String key, Integer value) {
		return setValue(jedis, key, wrapNull(value, ""));
	}

	public boolean setLong(Jedis jedis, String key, Long value) {
		return setValue(jedis, key, wrapNull(value, ""));
	}

	public boolean setExpirableInt(Jedis jedis, String key, Integer value) {
		return setExpirableValue(jedis, key, wrapNull(value, ""), ttl);
	}

	public boolean setExpirableShort(Jedis jedis, String key, Short value) {
		return setExpirableValue(jedis, key, wrapNull(value, ""), ttl);
	}

	public boolean setExpirableLong(Jedis jedis, String key, Long value) {
		return setExpirableValue(jedis, key, wrapNull(value, ""), ttl);
	}

	public boolean setExpirableValue(Jedis jedis, String key, String value) {
		return setExpirableValue(jedis, key, value, ttl);
	}

	public boolean setExpirableInt(Jedis jedis, String key, Integer value, int forcedTTL) {
		return setExpirableValue(jedis, key, wrapNull(value, ""), forcedTTL);
	}

	public boolean setExpirableLong(Jedis jedis, String key, Long value, int forcedTTL) {
		return setExpirableValue(jedis, key, wrapNull(value, ""), forcedTTL);
	}

	public void shutdown() {
		this.pool.close();
	}

	public static String wrapNull(Object source, String nullString) {
		return source == null ? nullString : source.toString();
	}
}
