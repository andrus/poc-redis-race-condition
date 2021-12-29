package poc.redis.race.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.bootique.cli.Cli;
import io.bootique.jdbc.DataSourceFactory;
import redis.clients.jedis.Jedis;

@Path("/")
public class ScoreApi {

	@Inject
	private Cli cli;

	private static final long ITEM_NOT_FOUND = -100;
	private static final long DB_RETRIEVE_RESULT_ERROR = -101;
	private static final long DB_CONNECTION_ERROR = -102;
	private static final long DUBLICATE_IN_DB_ERROR = -103;

	private final Timer responseTimer;
	private final Timer cacheSearchTimer;
	private final Timer dbSearchTimer;
	private final Timer cacheWriteTimer;

	private final Counter requestCounter;
	private final Counter cacheHitCounter;
	private final Counter cacheMissCounter;
	private final Counter cacheWritesCounter;
	private final Counter cacheWriteFailesCounter;

	@Inject
	private DataSourceFactory dataSourceFactory;

	@Inject
	public ScoreApi(MetricRegistry metrics) {
		this.responseTimer = metrics.timer("response-timer");
		this.cacheSearchTimer = metrics.timer("cache-search-timer");
		this.cacheWriteTimer = metrics.timer("cache-write-timer");
		this.dbSearchTimer = metrics.timer("db-search-timer");
		this.requestCounter = metrics.counter("request-counter");
		this.cacheHitCounter = metrics.counter("cache-hit-counter");
		this.cacheMissCounter = metrics.counter("cache-miss-counter");
		this.cacheWritesCounter = metrics.counter("cache-write-counter");
		this.cacheWriteFailesCounter = metrics.counter("cache-write-failes-counter");
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String get(@PathParam("id") String id) {
		this.requestCounter.inc();
		return "{\"score\":" + getScoreByID(id) + "}";
	}

	private long _getScoreByID(Jedis jedis, String id){
		long score = getScoreFromCache(jedis, id);
		if (score == ITEM_NOT_FOUND) {
			score = getScoreFromDB(id);
			if (score != ITEM_NOT_FOUND) {
				if (!putScoreToCache(jedis, id, score)) {
					cacheWriteFailesCounter.inc();
					/* 
						Re-read value if we failed to write (potentially we can dive deep into recursion, but this case 
						must be really hard to reproduce (we need to hit re-read exactly at expiry, which is not 
						realistic)
					*/
//					System.out.println("RE-READ"); // good for illustratoin in terminal
					return _getScoreByID(jedis, id);
				}
			}
		}
		return score;
	}
	
	private long getScoreByID(String id) {
		String singularityOption = cli.optionString("single-jedi");
		boolean singleJedi = false;
		if (singularityOption != null) {
			singleJedi = singularityOption.equals("true");
		}
		JedisCache cache = CacheHelper.getInstance();
		if (cache == null) {
			cache = CacheHelper.init(cli.optionString("jedi-strategy"));
		}
		Timer.Context context = this.responseTimer.time();
		try {
			if (singleJedi) {
				try (Jedis jedis = cache.allocJedis()) {
					return _getScoreByID(jedis, id);
				}
			} else {
				return _getScoreByID(null, id);
			}
		} finally {
			context.stop();
		}
	}

	private static final String GET_SCORES_STATEMENT = "SELECT score FROM scores WHERE id = ?";

	private boolean putScoreToCache(Jedis jedis, String id, Long score) {
		Timer.Context context = this.cacheWriteTimer.time();
		try {
			this.cacheWritesCounter.inc();
			if (jedis == null) {
				return CacheHelper.getInstance().setExpirableLong(id, score);
			} else {
				return CacheHelper.getInstance().setExpirableLong(jedis, id, score);
			}
		} finally {
			context.stop();
		}
	}

	private long getScoreFromCache(Jedis jedis, String id) {
		Timer.Context context = this.cacheSearchTimer.time();
		try {
			Long score;
			if (jedis == null) {
				score = CacheHelper.getInstance().getLong(id);
			} else {
				score = CacheHelper.getInstance().getLong(jedis, id);
			}
			if (score == null) {
				this.cacheMissCounter.inc();
				return ITEM_NOT_FOUND;
			}
			this.cacheHitCounter.inc();
			return score;
		} finally {
			context.stop();
		}
	}

	private long getScoreFromDB(String id) {
		Timer.Context context = this.dbSearchTimer.time();
		try {
			try (Connection connection = dataSourceFactory.forName("database").getConnection()) {
				PreparedStatement statement = connection.prepareStatement(GET_SCORES_STATEMENT);
				statement.setString(1, id);
				try {
					ResultSet result = statement.executeQuery();
					if (result != null) {
						long gameScore = ITEM_NOT_FOUND;
						while (result.next()) {
							if (gameScore != ITEM_NOT_FOUND) {
								return DUBLICATE_IN_DB_ERROR;
							} else {
								gameScore = result.getLong(1);
							}
						}
						return gameScore;
					} else {
						return DB_RETRIEVE_RESULT_ERROR;
					}
				} finally {
					statement.close();
					connection.close();
				}
			} catch (SQLException ex) {
				Logger.getLogger(ScoreApi.class.getName()).log(Level.SEVERE, null, ex);
				return DB_CONNECTION_ERROR;
			}
		} finally {
			context.stop();
		}
	}

}
