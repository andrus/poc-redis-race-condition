package poc.redis.race.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poc.redis.race.service.impl.BlindWrite;
import poc.redis.race.service.impl.CheckAndSet;
import poc.redis.race.service.impl.DummyWatchAndWrite;
import poc.redis.race.service.impl.PessimisticLock;
import poc.redis.race.service.impl.TransactionWatchWrite;
import poc.redis.race.service.impl.WatchTransactionWrite;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public abstract class JedisCache {

	enum ImplementationVariant {
		BLIND_WRITE,
		DUMMY_WATCH_AND_WRITE,
		WATCH_TRANSACTION_WRITE,
		TRANSACTION_WATCH_WRITE,
		CHECK_AND_SET,
		PESSIMISTIC_LOCK
	}

	private static JedisCache instance;
	public static final int DEFAULT_REDIS_TTL_MSEC = 300;
	public static final String OK = "OK";

	private final int ttl;
	private JedisPool pool = null;
	private Thread monitorThread = null;
	private Jedis monitorJedis = null;

	protected JedisCache(String host, int port, int ttl, String loggerPrefix, boolean enablePoolTests) {
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
		return init(host, port, ttl, null, false, ImplementationVariant.BLIND_WRITE);
	}

	public static JedisCache init(String host, int port, int ttl, String loggerPrefix, boolean enablePoolTests, ImplementationVariant variant) {
		if (instance == null) {
			switch (variant) {
				case BLIND_WRITE: {
					instance = new BlindWrite(host, port, ttl, loggerPrefix, enablePoolTests);
					break;
				}
				case DUMMY_WATCH_AND_WRITE: {
					instance = new DummyWatchAndWrite(host, port, ttl, loggerPrefix, enablePoolTests);
					break;
				}
				case WATCH_TRANSACTION_WRITE: {
					instance = new WatchTransactionWrite(host, port, ttl, loggerPrefix, enablePoolTests);
					break;
				}
				case TRANSACTION_WATCH_WRITE: {
					instance = new TransactionWatchWrite(host, port, ttl, loggerPrefix, enablePoolTests);
					break;
				}
				case CHECK_AND_SET: {
					instance = new CheckAndSet(host, port, ttl, loggerPrefix, enablePoolTests);
					break;
				}
				case PESSIMISTIC_LOCK: {
					instance = new PessimisticLock(host, port, ttl, loggerPrefix, enablePoolTests);
					break;
				}
			}
			return instance;
		} else {
			instance.pool.close();
			instance = null;
			return init(host, port, ttl, loggerPrefix, enablePoolTests, variant);
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
		this.monitorThread.interrupt();
		this.pool.close();
		instance = null;
	}

	public static String wrapNull(Object source, String nullString) {
		return source == null ? nullString : source.toString();
	}
}
