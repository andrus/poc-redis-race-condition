package poc.redis.race.service;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class RedisContainer extends GenericContainer<RedisContainer> {
	
	private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("redis");
	public static final String DEFAULT_TAG = "6";
	public static final Integer DEFAULT_PORT = 6379;
	
	public RedisContainer() {
		this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
	}
	
	private RedisContainer(final DockerImageName dockerImageName) {
		super(dockerImageName);
		dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
		addExposedPort(DEFAULT_PORT);
		setWaitStrategy(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
	}
	
}
