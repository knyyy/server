package edu.ucla.cens.awserver.dao;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import edu.ucla.cens.awserver.request.AwRequest;

/**
 * @author selsky
 */
public class FindAllCampaignsForUserDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(FindAllCampaignsForUserDao.class);
	private String _sql = "SELECT c.name " +
			              "FROM campaign c, user_role_campaign urc " +
			              "WHERE urc.campaign_id = c.id AND urc.user_id = ?";
	
	public FindAllCampaignsForUserDao(DataSource dataSource) {
		super(dataSource);
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		try {
			awRequest.setResultList(
				getJdbcTemplate().query(_sql, new Object[]{awRequest.getUser().getId()}, new SingleColumnRowMapper())
			);
		}	
		catch (org.springframework.dao.DataAccessException dae) {
			_logger.error("a DataAccessException occurred when running the following sql '" + _sql + "' with the parameter"
				+ awRequest.getUser().getId(), dae);
			throw new DataAccessException(dae);
		}
	}
}
