package poc.redis.race.service;

import java.util.ArrayList;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import poc.redis.race.service.JedisCache.ImplementationVariant;

public class RedisTestWrapper {

	private static final int DEFAULT_REDIS_PORT = 6379;

	private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("redis:6");

	ArrayList<JedisCache> caches;
	ImplementationVariant variant;
	
	String address;
	Integer port;
	int ttl;
	boolean running;

	public RedisTestWrapper() {
		this(JedisCache.DEFAULT_REDIS_TTL_MSEC, ImplementationVariant.BLIND_WRITE);
	}
	
	public RedisTestWrapper(ImplementationVariant variant) {
		this(JedisCache.DEFAULT_REDIS_TTL_MSEC, variant);
	}

	public RedisTestWrapper(int ttl, ImplementationVariant variant) {
		this(DEFAULT_REDIS_PORT, ttl, variant);
	}

	public RedisTestWrapper(int port, int ttl, ImplementationVariant variant) {
		this.port = port;
		this.ttl = ttl;
		this.variant = variant;
		this.caches = new ArrayList<JedisCache>();  
	}

	@SuppressWarnings("rawtypes")
	public void wrap(Runnable runnable) {
		running = true;
		try (GenericContainer redis = new GenericContainer(DOCKER_IMAGE_NAME)
				.withExposedPorts(port)
				.waitingFor(Wait.forListeningPort())) {
			redis.start();
			address = redis.getHost();
			port = redis.getFirstMappedPort();
			runnable.run();
			for (JedisCache cache : caches) {
				cache.shutdown();
			}
			redis.close();
		} catch (Exception e) {
		}
		running = false;

	}

	public String getAddress() {
		return address;
	}

	public Integer getPort() {
		return port;
	}
	
	public JedisCache getCache(int ttl, String loggerPrefix, boolean enablePoolTests) {
		JedisCache cache = JedisCache.init(address, port, ttl, loggerPrefix, enablePoolTests, this.variant);
		this.caches.add(cache);
		return cache;
	}
	
	public JedisCache getCache(int ttl, String loggerPrefix) {
		return getCache(ttl, loggerPrefix, false);
	}
	
	public JedisCache getCache(String loggerPrefix) {
		return getCache(ttl, loggerPrefix);
	}
	
	public JedisCache getCache() {
		return getCache(null);
	}

	public int getTtl() {
		return ttl;
	}

	public boolean isRunning() {
		return running;
	}

}