package edu.ucla.cens.awserver.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import edu.ucla.cens.awserver.domain.PromptType;

/**
 * Dao for retrieving the types and ids out of the prompt_type table.
 * 
 * TODO this class is nearly identical to UserRoleDao
 * 
 * @author selsky
 */
public class PromptTypeDao implements ParameterLessDao {
	private JdbcTemplate _jdbcTemplate;
	private String _sql = "select id, type from prompt_type";
	private static Logger logger = Logger.getLogger(PromptTypeDao.class);
	
	/**
	 * @throws IllegalArgumentException if the provided DataSource is null
	 */
	public PromptTypeDao(DataSource dataSource) {
		_jdbcTemplate = new JdbcTemplate(dataSource); 
	}
	
	/**
	 * @return a list of all of the user roles found in the database
	 */
	@Override
	public List<?> execute() {
		try {
			
			return _jdbcTemplate.query(_sql, new RowMapper() {
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
					PromptType pt = new PromptType();
					pt.setId(rs.getInt(1));
					pt.setType(rs.getString(2));
					return pt;
				}
			});
			
		} catch (org.springframework.dao.DataAccessException dae) {
			
			logger.error("an error occurred when attempting to run the following SQL: " + _sql);
			throw new DataAccessException(dae.getMessage());
		
		}
	}
}
