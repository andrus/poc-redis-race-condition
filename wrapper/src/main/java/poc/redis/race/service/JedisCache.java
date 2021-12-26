package poc.redis.race.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisCache {

	private static JedisCache instance;
	public static final int DEFAULT_REDIS_TTL_MSEC = 300;
	public static final String OK = "OK";

	private final int ttl;
	private JedisPool pool = null;
	private Thread monitorThread = null;
	private Jedis monitorJedis = null;

	private JedisCache(String host, int port, int ttl, String loggerPrefix, boolean enablePoolTests) {
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
		if (loggerPrefix != null) {
			final Logger monitorLogger = LoggerFactory.getLogger("🔮 [" + loggerPrefix + "]️");

			monitorThread = new Thread(new Runnable() {
				public void run() {
					monitorJedis = allocJedis();
					monitorJedis.monitor(new JedisMonitor() {

						@Override
						public void proceed(Connection client) {
							this.client = client;
							this.client.setTimeoutInfinite();
							while (client.isConnected()) {
								try {
									String command = client.getBulkReply();
									onCommand(command);
								} catch (JedisConnectionException jce) {
								}
							}
						}

						@Override
						public void onCommand(String command) {
							monitorLogger.info(command);
						}
					});
				}
			}, "wtch");
			monitorThread.start();
		}
		this.ttl = ttl;
	}

	public static JedisCache init(String host, int port) {
		return init(host, port, DEFAULT_REDIS_TTL_MSEC);
	}

	public static JedisCache init(String host, int port, int ttl) {
		return init(host, port, ttl, null, false);
	}

	public static JedisCache init(String host, int port, int ttl, String loggerPrefix, boolean enablePoolTests) {
		if (instance == null) {
			instance = new JedisCache(host, port, ttl, loggerPrefix, enablePoolTests);
			return instance;
		} else {
			instance.pool.close();
			instance = null;
			return init(host, port, ttl, loggerPrefix, enablePoolTests);
		}
	}

	public static JedisCache getInstance() {
		return instance;
	}

	public Set<String> keys() {
		try (Jedis jedis = allocJedis()) {
			return jedis.keys("*");
		}
	}

	private Jedis allocJedis() {
		if (pool == null) {
			throw new RuntimeException("JedisCache needs to be init() before any activity");
		}
		return pool.getResource();
	}

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

	public String getValue(String key) {
		try (Jedis jedis = allocJedis()) {
			String score = jedis.get(key);
			if (score != null) {
				return score;
			}
		}
		return null;
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

	public boolean setValue(String key, String value) {
		if (value == null) {
			value = "";
		}
		try (Jedis jedis = allocJedis()) {
			return jedis.set(key, value).equals(OK);
		}
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

	public boolean setExpirableValue(String key, String value, int forcedTTL) {
		try (Jedis jedis = allocJedis()) {
			if (jedis.set(key, value).equals(OK)) {
				return jedis.pexpire(key, forcedTTL) == 1L;
			} else {
				return false;
			}
		}
	}

	public void shutdown() {
		this.monitorJedis.close();
		this.monitorThread.interrupt();
		this.pool.close();
		instance = null;
	}

	public static String wrapNull(Object source, String nullString) {
		return source == null ? nullString : source.toString();
	}
}
