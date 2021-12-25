package poc.redis.race.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisCache {
	
	private static JedisCache instance;
	
	private int ttl;
	private JedisPool pool;

	private JedisCache (String host, int port, int ttl) {
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
		this.ttl = ttl;
	}
	
	public static JedisCache init (String host, int port,int ttl) {
		if (instance == null) {
			instance = new JedisCache(host, port, ttl);
			return instance;
		} else {
			instance.pool.close();
			instance = null;
			return init(host, port, ttl);
		}
	}
	
	public static JedisCache getInstance () {
		return instance;
	}

	public Short getScore(String id) {
		Jedis jedis = pool.getResource();
		String score = jedis.get(id);
		pool.returnResource(jedis);
		if (score != null) {
			try {
				return Short.parseShort(score);
			} catch (Exception ex) {}
		}
		return null;
	}
	
	// Oh my, this is a very bad itead, man. We push somethin' and than tell Redis to expire it, but there'll be a lag 
	// between two commands - nay... :)
	public boolean setScore (String id, Short score) {
		Jedis jedis = pool.getResource();
		String status = jedis.set(id, String.valueOf(score));
		jedis.pexpire(id, ttl);
		pool.returnResource(jedis);
		return status.equals("OK");
	}

	public void shutdown() {
		this.pool.close();
		instance = null;
	}
	
}
