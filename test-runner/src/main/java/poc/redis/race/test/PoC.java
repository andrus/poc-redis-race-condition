package poc.redis.race.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import poc.redis.race.test.CacheState.ValueState;

public class PoC {

	private static final int DEFAULT_TABLE_SIZE = 2;
	private static final int CLIENT_PARALLELISM = 2;
	private static final int UPDATE_EVERY_MSEC = 300;

	private static final short MAX_SCORE = 20;
	private static final short MIN_SCORE = 0;

	private static final String TRUNCATE_TABLE_STATEMENT = "TRUNCATE TABLE `scores`";
	private static final String INSERT_STATEMENT = "INSERT INTO `scores` VALUES (?, ?)";
	private static final String UPDATE_STATEMENT = "UPDATE `scores` SET score = ? WHERE id = ?";
	private static final Random RANDOM = new Random(System.currentTimeMillis());

	private Connection dbConnection = null;
	private HashMap<String, CacheState> targetScoresState;
	private List<String> targetIds;

	private static PoC instance;

	private Thread updaterThread;
	protected boolean updaterRunning;

	public static PoC getInstance() {
		if (instance == null) {
			instance = new PoC();
		}
		return instance;
	}

	private PoC() {
		try {
			dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:13306/database?", "user", "password");
			targetScoresState = new HashMap<String, CacheState>();
			targetIds = new LinkedList<String>();
			updaterRunning = false;
		} catch (SQLException ex) {
			Logger.getLogger(PoC.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public boolean clearTable() {
		try {
			PreparedStatement statement = dbConnection.prepareStatement(TRUNCATE_TABLE_STATEMENT);
			statement.execute();
			return true;
		} catch (SQLException ex) {
			Logger.getLogger(PoC.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	public boolean prefillTable() {
		return prefillTable(DEFAULT_TABLE_SIZE);
	}

	public boolean prefillTable(int size) {

		for (int i = 0; i < size; i++) {
			try {
				String id = UUID.randomUUID().toString();
				short value = (short) (MIN_SCORE + RANDOM.nextInt(MAX_SCORE - MIN_SCORE));

				PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT);
				statement.setString(1, id);
				statement.setInt(2, value);
				if (statement.executeUpdate() == 1) {
					this.targetIds.add(id);
					this.targetScoresState.put(id, new CacheState(id, value, CLIENT_PARALLELISM));
					System.out.println("new game '" + id + "' with score : " + value);
				}
			} catch (SQLException ex) {
				Logger.getLogger(PoC.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return true;
	}

	private void updateSomething() {
		try {
			for (String id : targetIds) {
				
				CacheState state = this.targetScoresState.get(id);
				if (state.getState() == ValueState.ACTUAL) {
					short value = nextVal(state.value(), RANDOM.nextBoolean());
					PreparedStatement statement = dbConnection.prepareStatement(UPDATE_STATEMENT);
					statement.setInt(1, value);
					statement.setString(2, id);
					if (statement.executeUpdate() == 1) {
						state.shedule(value);
					}
				}
			}
		} catch (SQLException ex) {
			Logger.getLogger(PoC.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void runUpdater() {
		if (!updaterRunning) {
			updaterThread = new Thread(new Runnable() {
				@Override
				public void run() {
					if (!updaterRunning) {
						updaterRunning = true;
						while (updaterRunning) {
							try {
								Thread.sleep(UPDATE_EVERY_MSEC);
								updateSomething();
							} catch (InterruptedException ex) {
								Logger.getLogger(PoC.class.getName()).log(Level.SEVERE, null, ex);
							}
						}
					}
				}
			});
			updaterThread.start();
		}
	}

	public void stopUpdater() {
		if (updaterRunning) {
			try {
				updaterRunning = false;
				updaterThread.join();
			} catch (InterruptedException ex) {
				Logger.getLogger(PoC.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private final short nextVal(int value, boolean add) {
		if (add) {
			return (value + 1 > MAX_SCORE) ? MAX_SCORE : ((short) (value + 1));
		} else {
			return (value - 1 < MIN_SCORE) ? MIN_SCORE : ((short) (value - 1));
		}
	}

	public List<String> getIds() {
		return this.targetIds;
	}

	public CacheState getCacheState(String id) {
		return this.targetScoresState.get(id);
	}

}
