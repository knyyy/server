package edu.ucla.cens.awserver.dao;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import edu.ucla.cens.awserver.domain.UserPercentage;
import edu.ucla.cens.awserver.domain.UserStatsQueryResult;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.UserStatsQueryAwRequest;

/**
 * DAO for counting the number of successful mobility location updates for the previous 24 hours (based on server-time) for a 
 * particular user or a group of users. 
 * 
 * A successful location update is defined by non-null latitude and longitude values in the mobility_mode_only_entry table.
 * 
 * @author selsky
 */
public class PastDayMobilityLocationUpdatesQueryDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(PastDayMobilityLocationUpdatesQueryDao.class);
	
	private String _nonNullPointsSql = "SELECT COUNT(*)" +
			                           " FROM mobility_mode_only_entry m, user u" +
			                           " WHERE m.user_id = u.id" +
			                           " AND u.login_ud = ?" +
			                           " AND DATE(msg_timestamp) BETWEEN DATE((now() - 1)) and DATE(now())" +
			                           " AND latitude is not NULL" +
			                           " AND longitude is not NULL";      
	
	private String _totalPointsSql = "SELECT COUNT(*)" +
				                     " FROM mobility_mode_only_entry m, user u" +
						             " WHERE m.user_id = u.id" +
						             " AND u.login_id = ?" +
						             " AND DATE(msg_timestamp) BETWEEN DATE(now() - 1) and DATE(now())";

	public PastDayMobilityLocationUpdatesQueryDao(DataSource dataSource) {
		super(dataSource);
	}
	
	/**
	 * Calculates the percentage of successful location updates for the user id found in the provided AwRequest.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		UserStatsQueryAwRequest req = (UserStatsQueryAwRequest) awRequest; //TODO should do an instanceof check here
		
		Object[] paramArray = {req.getUserNameRequestParam(), req.getCampaignName()};
		double totalSuccess = 0d;
		double total = 0d;
		String currentSql =_totalPointsSql; 
		UserPercentage userPercentage = null;
		UserStatsQueryResult userStatsQueryResult = null;
		
		if(null == req.getUserStatsQueryResult()) {
			userStatsQueryResult = new UserStatsQueryResult();
		} else {
			userStatsQueryResult = req.getUserStatsQueryResult();
		}
		
		try {
			
			total = getJdbcTemplate().queryForInt(currentSql, paramArray);
			
			if(0 == total) {
				
				userPercentage = new UserPercentage(req.getUserNameRequestParam(), 0d);
				
			} else {
				
				currentSql = _nonNullPointsSql;
				totalSuccess = getJdbcTemplate().queryForInt(currentSql, paramArray);
				userPercentage = new UserPercentage(req.getUserNameRequestParam(), (totalSuccess / total));
			}
			
			// the line below, the SQL being run, and the SQL params are the only items that differ from PastDaySurveyLocationUpdatesQueryDao
			userStatsQueryResult.setMobilityLocationUpdatesPercentage(userPercentage);
			
		} catch (IncorrectResultSizeDataAccessException irsdae) { // thrown if queryForInt returns more than one row which means 
                                                                  // there is a logical error in the SQL being run

			_logger.error("an incorrect number of rows was returned by '" + currentSql + "' with parameters " + paramArray);
			throw new DataAccessException(irsdae);

		} catch (org.springframework.dao.DataAccessException dae) { // thrown for general SQL errors

			_logger.error("an error was encountered when executing the following SQL: " + currentSql + " with parameters " 
				+ paramArray);
			throw new DataAccessException(dae);
		}
	}
}
