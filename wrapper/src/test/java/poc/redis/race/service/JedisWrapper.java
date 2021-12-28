package poc.redis.race.service;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poc.redis.race.service.JedisCache.ImplementationVariant;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisWrapper {

	final Logger monitorLogger = LoggerFactory.getLogger("🔮 [monitor]️");
	private ArrayList<JedisCache> caches;
	private ImplementationVariant variant;

	private Thread monitorThread = null;
	private Jedis monitorJedis = null;

	private String address;
	private Integer port;
	private int ttl;

	public JedisWrapper(RedisContainer redis) {
		this(redis, JedisCache.DEFAULT_REDIS_TTL_MSEC, ImplementationVariant.BLIND_WRITE);
	}

	public JedisWrapper(RedisContainer redis, int ttl, ImplementationVariant variant) {
		this.ttl = ttl;
		this.variant = variant;
		this.caches = new ArrayList<JedisCache>();
		this.address = redis.getHost();
		this.port = redis.getFirstMappedPort();
		monitorThread = new Thread(() -> {
			monitorJedis = new Jedis(address, port, 0);
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
		}, "wtch");
		monitorThread.start();
	}

	public JedisCache getCache(int ttl, boolean enablePoolTests) {
		JedisCache cache = JedisCache.init(address, port, ttl, enablePoolTests, this.variant);
		this.caches.add(cache);
		return cache;
	}

	public JedisCache getCache(int ttl) {
		return getCache(ttl, false);
	}

	public JedisCache getCache() {
		return getCache(ttl);
	}


	public void shutdown() {
		if (monitorThread != null && monitorThread.isAlive()){
			monitorThread.interrupt();
			monitorJedis.close();
		} 
		this.caches.forEach(cache -> {
			cache.shutdown();
		});
	}

	public void flushAll() {
		try (Jedis temp = new Jedis(address, port, 0)) {
			temp.flushAll();
		}
	}

	void setVariant(ImplementationVariant variant) {
		this.variant = variant;
	}

}
