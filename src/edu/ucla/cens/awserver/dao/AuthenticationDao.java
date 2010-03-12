package edu.ucla.cens.awserver.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import jbcrypt.BCrypt;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * DAO for performing user authentication. Incoming passwords are salted and hashed using bcrypt. The salt must be shared 
 * between the server and the phone.
 * 
 * @author selsky
 */
public class AuthenticationDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(AuthenticationDao.class);
	private String _salt;
	
	private static final String _selectSql = "select user.id, user.enabled, user.new_account, campaign.id, " +
			                                 "user_role_campaign.user_role_id " +
			                                 "from campaign, user, user_role_campaign " +
			                                 "where user.login_id = ? " +
			                                     "and user.password = ? " + 
			                                     "and user.id = user_role_campaign.user_id " +
			                                     "and campaign.id = user_role_campaign.campaign_id";
	
	/**
	 * @param dataSource the data source used to connect to the MySQL db
	 * @param salt the salt to use for password hashing with bcrypt 
	 * 
	 * @throws IllegalArgumentException if the provided salt is empty, null, or all whitespace
	 */
	public AuthenticationDao(DataSource dataSource, String salt) {
		super(dataSource);
		
		if(StringUtils.isEmptyOrWhitespaceOnly(salt)) {
			throw new IllegalArgumentException("a salt value is required");
		}
		
		_salt = salt;
	}
	
	/**
	 * Checks the db for the existence of a user represented by a user name and a subdomain found in the AwRequest. Places the 
	 * query results into LoginResult objects.
	 */
	public void execute(AwRequest awRequest) {
		_logger.info("attempting login for user " + awRequest.getUser().getUserName());
		
		try {
			awRequest.setResultList(getJdbcTemplate().query(_selectSql, 
					             new String[]{awRequest.getUser().getUserName(), 
					                          BCrypt.hashpw(awRequest.getUser().getPassword(), _salt), 
					             }, 
					             new AuthenticationResultRowMapper()));
			
		} catch (org.springframework.dao.DataAccessException dae) {
			
			_logger.error("caught DataAccessException when running SQL '" + _selectSql + "' with the following parameters: " + 
					awRequest.getUser().getUserName() + " (password omitted)");
			
			throw new DataAccessException(dae); // Wrap the Spring exception and re-throw in order to avoid outside dependencies
			                                    // on the Spring Exception (in case Spring JDBC is replaced with another lib in 
			                                    // the future).
		}
	}

	/**
	 * Maps each row from a query ResultSet to a LoginResult. Used by JdbcTemplate in call-back fashion. 
	 * 
	 * @author selsky
	 */
	public class AuthenticationResultRowMapper implements RowMapper {
		
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException { // The Spring classes will wrap this exception
			                                                                 // in a Spring DataAccessException
			LoginResult lr = new LoginResult();
			lr.setUserId(rs.getInt(1));
			lr.setEnabled(rs.getBoolean(2));
			lr.setNew(rs.getBoolean(3));
			lr.setCampaignId(rs.getInt(4));
			lr.setUserRoleId(rs.getInt(5));
			return lr;
		}
	}
	
	/**
	 * Container used for query results.
	 * 
	 * @author selsky
	 */
	public class LoginResult {
		private int _campaignId;
		private int _userRoleId;
		private int _userId;
		private boolean _enabled;
		private boolean _new;
		
		public int getCampaignId() {
			return _campaignId;
		}
		public void setCampaignId(int campaignId) {
			_campaignId = campaignId;
		}
		public int getUserId() {
			return _userId;
		}
		public void setUserId(int userId) {
			_userId = userId;
		}
		public boolean isEnabled() {
			return _enabled;
		}
		public void setEnabled(boolean enabled) {
			_enabled = enabled;
		}
		public boolean isNew() {
			return _new;
		}
		public void setNew(boolean bnew) {
			_new = bnew;
		}
		public int getUserRoleId() {
			return _userRoleId;
		}
		public void setUserRoleId(int userRoleId) {
			_userRoleId = userRoleId;
		}
	}
}
