package edu.ucla.cens.awserver.dao;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import edu.ucla.cens.awserver.request.AwRequest;

/**
 * DAO for retrieving the campaigns a user belongs to.
 * 
 * 
 * 
 * This Service checks that the logged in user (AwRequest.User) belongs to the campaign found in the AwRequest. It's basically a 
 * paranoid sanity check to make sure that token-authenticated users aren't playing tricks and attempting to access data from
 * campaigns they don't belong to. (e.g, by changing the campaign id and version POSTed to the data point API.)
 * 
 * @author selsky
 */
public class UserCampaignCheckDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(UserCampaignCheckDao.class);
	private String _sql = "SELECT DISTINCT campaign_id FROM user_role_campaign WHERE user_id = ?";
	
	public UserCampaignCheckDao(DataSource dataSource) {
		super(dataSource);
	}

	public void execute(AwRequest awRequest) {
		
		try {
		
			awRequest.setResultList(
				getJdbcTemplate().query(_sql, new Object[] {awRequest.getUser().getId()}, new SingleColumnRowMapper())
			);
		
		} catch (org.springframework.dao.DataAccessException dae) {
			
			_logger.error("An error occurred running the following SQL '" + _sql + "' with the parameter '" 
				+ awRequest.getUser().getId() + "'", dae);
			throw new DataAccessException(dae);
		}
	}
}
