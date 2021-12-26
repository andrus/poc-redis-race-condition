package poc.redis.race.service;

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

	private JedisCache(String host, int port, int ttl, boolean withMonitor) {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(128);
		config.setMaxIdle(128);
		config.setMinIdle(16);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setTestWhileIdle(true);
		config.setNumTestsPerEvictionRun(3);
		config.setBlockWhenExhausted(true);
		pool = new JedisPool(config, host, port);
		if (withMonitor) {
			final Logger monitorLogger = LoggerFactory.getLogger("🔮 [command]️");

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
		return init(host, port, DEFAULT_REDIS_TTL_MSEC, true);
	}

	public static JedisCache init(String host, int port, int ttl) {
		return init(host, port, ttl, true);
	}

	public static JedisCache init(String host, int port, int ttl, boolean withMonitor) {
		if (instance == null) {
			instance = new JedisCache(host, port, ttl, withMonitor);
			return instance;
		} else {
			instance.pool.close();
			instance = null;
			return init(host, port, ttl, withMonitor);
		}
	}

	public static JedisCache getInstance() {
		return instance;
	}

	private Jedis allocJedis() {
		if (pool == null) {
			throw new RuntimeException("JedisCache needs to be init() before any activity");
		}
		return pool.getResource();
	}

	public Short getScore(String id) {
		try (Jedis jedis = allocJedis()) {
			String score = jedis.get(id);
			if (score != null) {
				try {
					return Short.parseShort(score);
				} catch (NumberFormatException ex) {
				}
			}
		}
		return null;
	}

	public boolean setScore(String id, Short score) {
		try (Jedis jedis = allocJedis()) {
			return jedis.set(id, String.valueOf(score)).equals(OK);
		}
	}

	public boolean setExpirableScore(String id, Short score) {
		return setExpirableScore(id, score, ttl);
	}

	public boolean setExpirableScore(String id, Short score, int forcedTTL) {
		try (Jedis jedis = allocJedis()) {
			if (jedis.set(id, String.valueOf(score)).equals(OK)) {
				return jedis.pexpire(id, forcedTTL) == 1L;
			} else {
				return false;
			}
		}
	}

	public void shutdown() {
		this.monitorJedis.close();
		this.pool.close();
		instance = null;
	}

}
