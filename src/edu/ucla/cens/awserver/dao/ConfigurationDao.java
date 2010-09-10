package edu.ucla.cens.awserver.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import edu.ucla.cens.awserver.domain.Configuration;

/**
 * Configuration data access object.
 * 
 * @author selsky
 */
public class ConfigurationDao extends AbstractParameterLessDao {
	private static Logger _logger = Logger.getLogger(ConfigurationDao.class);
	private String _sql = "select c.name, cc.version, cc.xml" +
		                  " from campaign c, campaign_configuration cc" +
		                  " where c.id = cc.campaign_id";
	
	public ConfigurationDao(DataSource dataSource) {
		super(dataSource);
	}
	
	/**
	 * Returns a list of campaign configurations.
	 */
	@Override
	public List<?> execute() {
		try {
			return _jdbcTemplate.query(_sql, new RowMapper() {
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
					String campaignName = rs.getString(1);
					String campaignVersion = rs.getString(2);
					String xml = rs.getString(3);
					return new Configuration(campaignName, campaignVersion, xml);
				}
		    });
			
		} catch (org.springframework.dao.DataAccessException dae) {
				
			_logger.error("an exception occurred running the sql '" + _sql + "' " + dae.getMessage());
			throw new DataAccessException(dae);
		}
	}
}
