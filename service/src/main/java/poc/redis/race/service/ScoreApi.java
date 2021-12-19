package poc.redis.race.service;


import io.bootique.jdbc.DataSourceFactory;
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

@Path("/")
public class ScoreApi {

	private static final short ITEM_NOT_FOUND = -100;
	private static final short DB_RETRIEVE_RESULT_ERROR = -101;
	private static final short DB_CONNECTION_ERROR = -102;
	private static final short DUBLICATE_IN_DB_ERROR = -103;
	
	@Inject
	private DataSourceFactory dataSourceFactory;
	
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public String get(@PathParam("id") String id) {
		return "{\"score\":" + getScoreByID(id) + "}";
	}

	private short getScoreByID (String id) {
		
		return getScoreFromDB(id);
	}
	
	private static final String GET_SCORES_STATEMENT = "SELECT score FROM scores WHERE id = ?";
	
	private Short getScoreFromDB (String id) {
		try (Connection connection = dataSourceFactory.forName("database").getConnection()) {
			PreparedStatement statement = connection.prepareStatement(GET_SCORES_STATEMENT);
			statement.setString(1, id);
			try {
				ResultSet result = statement.executeQuery();
				if (result != null) {
					short gameScore = ITEM_NOT_FOUND;
					while (result.next()) {
						if (gameScore != ITEM_NOT_FOUND){
							return DUBLICATE_IN_DB_ERROR;
						} else {
							gameScore = (short) result.getInt(1);
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
	}
	
}
