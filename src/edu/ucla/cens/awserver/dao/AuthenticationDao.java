package edu.ucla.cens.awserver.dao;

import javax.sql.DataSource;

import jbcrypt.BCrypt;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.request.AwRequest;

/**
 * DAO for performing user authentication.
 * 
 * @author selsky
 */
public class AuthenticationDao extends AbstractDao {
	private static Logger _logger = Logger.getLogger(AuthenticationDao.class);
	
	private static final String _selectSql = "SELECT user.id, user.enabled, user.new_account" 
		                                     + " FROM user"
		                                     + " WHERE user.login_id = ?"
		                                     +   " AND user.password = ?";
	
	private static final String SQL_GET_PASSWORD = "SELECT password " +
												   "FROM user " +
												   "WHERE login_id = ?";
	
	private boolean _performHash;
	
	/**
	 * @param dataSource the data source used to connect to the MySQL db
	 * @param salt the salt to use for password hashing with bcrypt
	 * @param performHash specifies whether hashing should be performed on the inbound password. Passwords sent from the phone
	 * are already hashed. 
	 * 
	 * @throws IllegalArgumentException if the provided salt is empty, null, or all whitespace and performHash is true
	 */
	public AuthenticationDao(DataSource dataSource, boolean performHash) {
		super(dataSource);
		
		_performHash = performHash;
	}
	
	/**
	 * Checks the db for the existence of a user represented by a user name and a subdomain found in the AwRequest. Places the 
	 * query results into LoginResult objects.
	 */
	public void execute(AwRequest awRequest) {
		_logger.info("attempting login for user " + awRequest.getUser());
		
		// If we are supposed to perform the hash, then we do so and update
		// the request's user's password (which should be plaintext but could
		// be anything) with a hash of that password and a salt of their 
		// actual password salted. If their plaintext password is correct,
		// this hashing will result in the same value as the salt (their
		// plaintext password correctly hashed).
		if(_performHash) {
			try {
				String password = (String) getJdbcTemplate().queryForObject(SQL_GET_PASSWORD, 
																			new Object[] { awRequest.getUser().getUserName() },
																			String.class);
				awRequest.getUser().setPassword(BCrypt.hashpw(awRequest.getUser().getPassword(), password));
			}
			catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
				if(e.getActualSize() > 1) {
					_logger.error("Data integrity issue on user table. More than one user with the same username.");
					throw new DataAccessException(e);
				}
				// If there weren't any users, return and let the service
				// handle the lack of results.
				return;
			}
			catch(org.springframework.dao.DataAccessException e) {
				_logger.error("Error while executing SQL '" + SQL_GET_PASSWORD + "' with parameter: " + awRequest.getUser().getPassword());
				throw new DataAccessException(e);
			}
		}
		
		try {
			awRequest.setResultList(
				getJdbcTemplate().query(
					_selectSql, 
					new String[] {
					    awRequest.getUser().getUserName(), 
					    awRequest.getUser().getPassword()
					}, 
					new AuthenticationResultRowMapper()
				)
			);
			
		} catch (org.springframework.dao.DataAccessException dae) {
			
			_logger.error("caught DataAccessException when running SQL '" + _selectSql + "' with the following parameters: " + 
					awRequest.getUser().getUserName() + " (password omitted)");
			
			throw new DataAccessException(dae); // Wrap the Spring exception and re-throw in order to avoid outside dependencies
			                                    // on the Spring Exception (in case Spring JDBC is replaced with another lib in 
			                                    // the future).
		}
	}
}
