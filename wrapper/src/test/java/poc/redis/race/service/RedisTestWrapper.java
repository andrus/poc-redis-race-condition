package poc.redis.race.service;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class RedisTestWrapper {

	private static final int DEFAULT_REDIS_PORT = 6379;

	private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("redis:6");

	String address;
	Integer port;
	JedisCache cache;
	int ttl;
	boolean running;

	public RedisTestWrapper() {
		this(JedisCache.DEFAULT_REDIS_TTL_MSEC);
	}

	public RedisTestWrapper(int ttl) {
		this(DEFAULT_REDIS_PORT, ttl);
	}

	public RedisTestWrapper(int port, int ttl) {
		this.port = port;
		this.ttl = ttl;
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
			cache = JedisCache.init(address, port, ttl, true);
			runnable.run();
			cache.shutdown();
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

	public JedisCache getCache() {
		return cache;
	}

	public int getTtl() {
		return ttl;
	}

	public boolean isRunning() {
		return running;
	}

}
