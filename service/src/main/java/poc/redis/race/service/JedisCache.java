package poc.redis.race.service;

import redis.clients.jedis.Jedis;

public class JedisCache {
	
	private static JedisCache instance;
	
	private int ttlMsec;
	private final Jedis jedis;
	
	private JedisCache (String host, int port, int ttlMsec) {
		this.jedis = new Jedis(host, port);
		this.ttlMsec = ttlMsec;
	}
	
	public static void init (String host, int port,int ttlMsec) {
		instance = new JedisCache(host, port, ttlMsec);
	}
	
	public static JedisCache getInstance () {
		return instance;
	}

	public Short getScore(String id) {
		String score = jedis.get(id);
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
		String status = jedis.set(id, String.valueOf(score));
		jedis.expireAt(id, System.currentTimeMillis() + ttlMsec);
		return status.equals("OK");
	}
	
}
