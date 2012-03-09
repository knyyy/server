/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.query.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.ohmage.cache.PreferenceCache;
import org.ohmage.domain.Clazz;
import org.ohmage.domain.UserInformation;
import org.ohmage.domain.UserInformation.UserPersonal;
import org.ohmage.domain.campaign.Campaign;
import org.ohmage.exception.CacheMissException;
import org.ohmage.exception.DataAccessException;
import org.ohmage.exception.DomainException;
import org.ohmage.query.IUserQueries;
import org.ohmage.query.impl.QueryResult.QueryResultBuilder;
import org.ohmage.util.StringUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * This class contains all of the functionality for creating, reading, 
 * updating, and deleting users. While it may read information pertaining to
 * other entities, the information it takes and provides should pertain to 
 * users only.
 * 
 * @author John Jenkins
 */
public class UserQueries extends Query implements IUserQueries {
	// Returns a boolean representing whether or not a user exists
	private static final String SQL_EXISTS_USER = 
		"SELECT EXISTS(" +
			"SELECT username " +
			"FROM user " +
			"WHERE username = ?" +
		")";
	
	// Returns a single, boolean row if the user exists which explains if the
	// user is an admin or not.
	private static final String SQL_EXISTS_USER_IS_ADMIN = 
		"SELECT admin " +
		"FROM user " +
		"WHERE username = ?";
	
	// Returns a single, boolean row if the user exists which explains if the
	// user's account is enabled.
	private static final String SQL_EXISTS_USER_IS_ENABLED = 
		"SELECT enabled " +
		"FROM user " +
		"WHERE username = ?";
	
	// Returns a single, boolean row if the user exists which explains if the
	// user's account is new.
	private static final String SQL_EXISTS_USER_IS_NEW_ACCOUNT = 
		"SELECT new_account " +
		"FROM user " +
		"WHERE username = ?";
	
	// Returns a boolean representing whether a user can create campaigns or 
	// not. If the user doesn't exist, false is returned.
	private static final String SQL_EXISTS_USER_CAN_CREATE_CAMPAIGNS =
		"SELECT EXISTS(" +
			"SELECT username " +
			"FROM user " +
			"WHERE username = ? " +
			"AND campaign_creation_privilege = true" +
		")";
	
	// Returns a boolean representing whether or not a user has a personal
	// information entry.
	private static final String SQL_EXISTS_USER_PERSONAL =
		"SELECT EXISTS(" +
			"SELECT user_id " +
			"FROM user_personal " +
			"WHERE user_id = (" +
				"SELECT Id " +
				"FROM user " +
				"WHERE username = ?" +
			")" +
		")";
	
	private static final String SQL_GET_ALL_USERNAMES =
		"SELECT username " +
		"FROM user";
	
	private static final String SQL_GET_USERNAMES_LIKE_USERNAME =
		"SELECT username " +
		"FROM user " +
		"WHERE username LIKE ?";
	
	private static final String SQL_GET_USERNAMES_WITH_ADMIN_VALUE =
		"SELECT username " +
		"FROM user " +
		"WHERE admin = ?";
	
	private static final String SQL_GET_USERNAMES_WITH_ENABLED_VALUE =
		"SELECT username " +
		"FROM user " +
		"WHERE enabled = ?";
	
	private static final String SQL_GET_USERNAMES_WITH_NEW_ACCOUNT_VALUE =
		"SELECT username " +
		"FROM user " +
		"WHERE new_account = ?";
	
	private static final String SQL_GET_USERNAMES_WITH_CAMPAIGN_CREATION_PRIVILEGE =
		"SELECT username " +
		"FROM user " +
		"WHERE campaign_creation_privilege = ?";
	
	private static final String SQL_GET_USERNAMES_LIKE_FIRST_NAME =
		"SELECT username " +
		"FROM user, user_personal " +
		"WHERE user.id = user_id " +
		"AND first_name LIKE ?";
	
	private static final String SQL_GET_USERNAMES_LIKE_LAST_NAME =
		"SELECT username " +
		"FROM user, user_personal " +
		"WHERE user.id = user_id " +
		"AND last_name LIKE ?";
	
	private static final String SQL_GET_USERNAMES_LIKE_ORGANIZATION =
		"SELECT username " +
		"FROM user, user_personal " +
		"WHERE user.id = user_id " +
		"AND organization LIKE ?";
	
	private static final String SQL_GET_USERNAMES_LIKE_PERSONAL_ID =
		"SELECT username " +
		"FROM user, user_personal " +
		"WHERE user.id = user_id " +
		"AND personal_id LIKE ?";
	
	private static final String SQL_GET_USERNAMES_LIKE_EMAIL_ADDRESS =
		"SELECT username " +
		"FROM user " +
		"WHERE email_address LIKE ?";
	
	// Retrieves the personal information about a user.
	private static final String SQL_GET_USER_PERSONAL =
		"SELECT up.first_name, up.last_name, up.organization, up.personal_id " +
		"FROM user u, user_personal up " +
		"WHERE u.username = ? " +
		"AND u.id = up.user_id";
	
	// Inserts a new user.
	private static final String SQL_INSERT_USER = 
		"INSERT INTO user(username, email_address, password, admin, enabled, new_account, campaign_creation_privilege) " +
		"VALUES (?,?,?,?,?,?)";
	
	// Inserts a new personal information record for a user. Note: this doesn't
	// insert the email address or JSON data; to add these, update the record
	// immediately after using this to insert it.
	private static final String SQL_INSERT_USER_PERSONAL =
		"INSERT INTO user_personal(user_id, first_name, last_name, organization, personal_id) " +
		"VALUES ((" +
			"SELECT id " +
			"FROM user " +
			"WHERE username = ?" +
		"),?,?,?,?)";
	
	// Updates the user's password.
	private static final String SQL_UPDATE_PASSWORD = 
		"UPDATE user " +
		"SET password = ? " +
		"WHERE username = ?";
	
	// Updates a user's admin value.
	private static final String SQL_UPDATE_ADMIN =
		"UPDATE user " +
		"SET admin = ? " +
		"WHERE username = ?";
	
	// Updates a user's enabled value.
	private static final String SQL_UPDATE_ENABLED =
		"UPDATE user " +
		"SET enabled = ? " +
		"WHERE username = ?";
	
	// Updates a user's new account value.
	private static final String SQL_UPDATE_NEW_ACCOUNT =
		"UPDATE user " +
		"SET new_account = ? " +
		"WHERE username = ?";
	
	// Updates a user's campaign creation privilege.
	private static final String SQL_UPDATE_CAMPAIGN_CREATION_PRIVILEGE =
		"UPDATE user " +
		"SET campaign_creation_privilege = ? " +
		"WHERE username = ?";
	
	// Updates a user's first name in their personal information record.
	private static final String SQL_UPDATE_FIRST_NAME = 
		"UPDATE user_personal " +
		"SET first_name = ? " +
		"WHERE user_id = (" +
			"SELECT Id " +
			"FROM user " +
			"WHERE username = ?" +
		")";
	
	// Updates a user's last name in their personal information record.
	private static final String SQL_UPDATE_LAST_NAME = 
		"UPDATE user_personal " +
		"SET last_name = ? " +
		"WHERE user_id = (" +
			"SELECT Id " +
			"FROM user " +
			"WHERE username = ?" +
		")";
	
	// Updates a user's organization in their personal information record.
	private static final String SQL_UPDATE_ORGANIZATION = 
		"UPDATE user_personal " +
		"SET organization = ? " +
		"WHERE user_id = (" +
			"SELECT Id " +
			"FROM user " +
			"WHERE username = ?" +
		")";
	
	// Updates a user's personal ID in their personal information record.
	private static final String SQL_UPDATE_PERSONAL_ID = 
		"UPDATE user_personal " +
		"SET personal_id = ? " +
		"WHERE user_id = (" +
			"SELECT Id " +
			"FROM user " +
			"WHERE username = ?" +
		")";
	
	// Updates a user's email address in their personal information record.
	private static final String SQL_UPDATE_EMAIL_ADDRESS = 
		"UPDATE user " +
		"SET email_address = ? " +
		"WHERE username = ?";
	
	// Deletes the user.
	private static final String SQL_DELETE_USER = 
		"DELETE FROM user " +
		"WHERE username = ?";
	
	/**
	 * Creates this object.
	 * 
	 * @param dataSource The DataSource to use to query the database.
	 */
	private UserQueries(final DataSource dataSource) {
		super(dataSource);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#createUser(java.lang.String, java.lang.String, java.lang.String, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean)
	 */
	public void createUser(String username, String hashedPassword, String emailAddress, Boolean admin, Boolean enabled, Boolean newAccount, Boolean campaignCreationPrivilege) 
		throws DataAccessException {
		
		Boolean tAdmin = admin;
		if(tAdmin == null) {
			tAdmin = false;
		}
		
		Boolean tEnabled = enabled;
		if(tEnabled == null) {
			tEnabled = false;
		}
		
		Boolean tNewAccount = newAccount;
		if(tNewAccount == null) {
			tNewAccount = true;
		}
		
		Boolean tCampaignCreationPrivilege = campaignCreationPrivilege;
		if(tCampaignCreationPrivilege == null) {
			try {
				tCampaignCreationPrivilege = PreferenceCache.instance().lookup(PreferenceCache.KEY_DEFAULT_CAN_CREATE_PRIVILIEGE).equals("true");
			}
			catch(CacheMissException e) {
				throw new DataAccessException("Cache doesn't know about 'known' value: " + PreferenceCache.KEY_DEFAULT_CAN_CREATE_PRIVILIEGE, e);
			}
		}
		
		// Create the transaction.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Creating a new user.");
		
		try {
			// Begin the transaction.
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def);
			
			// Insert the new user.
			try {
				getJdbcTemplate().update(SQL_INSERT_USER, new Object[] { username, emailAddress, hashedPassword, tAdmin, tEnabled, tNewAccount, tCampaignCreationPrivilege });
			}
			catch(org.springframework.dao.DataAccessException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while executing SQL '" + SQL_INSERT_USER + "' with parameters: " +
						username + ", " + emailAddress + ", " + hashedPassword + ", " + tAdmin + ", " + tEnabled + ", " + tNewAccount + ", " + tCampaignCreationPrivilege, e);
			}
			
			// Commit the transaction.
			try {
				transactionManager.commit(status);
			}
			catch(TransactionException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while committing the transaction.", e);
			}
		}
		catch(TransactionException e) {
			throw new DataAccessException("Error while attempting to rollback the transaction.", e);
		}
	}
	
	/**
	 * Returns whether or not a user exists.
	 * 
	 * @param username The username for which to check.
	 * 
	 * @return Returns true if the user exists; false, otherwise.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public Boolean userExists(String username) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER, 
					new Object[] { username }, 
					Boolean.class);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing the following SQL '" + SQL_EXISTS_USER + "' with parameter: " + username, e);
		}
	}
	
	/**
	 * Gets whether or not the user is an admin.
	 * 
	 * @param username The username to check.
	 * 
	 * @return Whether or not they are an admin.
	 * 
	 * @throws DataAccessException Thrown if there is a problem running the
	 * 							   query.
	 */
	public Boolean userIsAdmin(String username) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER_IS_ADMIN, 
					new String[] { username }, 
					Boolean.class
					);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing the following SQL '" + SQL_EXISTS_USER_IS_ADMIN + "' with parameter: " + username, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#userIsEnabled(java.lang.String)
	 */
	@Override
	public Boolean userIsEnabled(String username) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER_IS_ENABLED, 
					new String[] { username }, 
					Boolean.class
					);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing the following SQL '" + SQL_EXISTS_USER_IS_ENABLED + "' with parameter: " + username, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#userHasNewAccount(java.lang.String)
	 */
	@Override
	public Boolean userHasNewAccount(String username)
			throws DataAccessException {
		
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER_IS_NEW_ACCOUNT, 
					new String[] { username }, 
					Boolean.class
					);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing the following SQL '" + SQL_EXISTS_USER_IS_NEW_ACCOUNT + "' with parameter: " + username, e);
		}
	}
	
	/**
	 * Gets whether or not the user is allowed to create campaigns.
	 * 
	 * @param username The username of the user in question.
	 * 
	 * @return Whether or not the user can create campaigns.
	 * 
	 * @throws DataAccessException Thrown if there is a problem running the
	 * 							   query.
	 */
	public Boolean userCanCreateCampaigns(String username) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER_CAN_CREATE_CAMPAIGNS, 
					new Object[] { username }, 
					Boolean.class
					);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing the following SQL '" + SQL_EXISTS_USER_CAN_CREATE_CAMPAIGNS + "' with parameter: " + username, e);
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(UserQueries.class);
	
	/**
	 * Checks if a user has a personal information entry in the database.
	 *  
	 * @param username The username of the user.
	 * 
	 * @return Returns true if the user has a personal information entry; 
	 * 		   returns false otherwise.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public Boolean userHasPersonalInfo(String username) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_EXISTS_USER_PERSONAL,
					new Object[] { username },
					Boolean.class
					);
		}
		catch(org.springframework.dao.DataAccessException e) {
			LOGGER.error("Error executing the following SQL '" + SQL_EXISTS_USER_PERSONAL + "' with parameter: " + username, e);
			throw new DataAccessException("Error executing the following SQL '" + SQL_EXISTS_USER_PERSONAL + "' with parameter: " + username, e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getAllUsernames()
	 */
	@Override
	public List<String> getAllUsernames() throws DataAccessException {
		try {
			return getJdbcTemplate().query(
					SQL_GET_ALL_USERNAMES,
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_ALL_USERNAMES,
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesFromPartialUsername(java.lang.String)
	 */
	@Override
	public List<String> getUsernamesFromPartialUsername(String username)
			throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_LIKE_USERNAME, 
					new Object[] { "%" + username + "%" }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_LIKE_USERNAME +
						"' with parameter: " +
						"%" + username + "%",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesWithAdminValue(java.lang.Boolean)
	 */
	@Override
	public List<String> getUsernamesWithAdminValue(Boolean admin)
			throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_WITH_ADMIN_VALUE, 
					new Object[] { admin }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_WITH_ADMIN_VALUE +
						"' with parameter: " +
						admin,
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesWithEnabledValue(java.lang.Boolean)
	 */
	@Override
	public List<String> getUsernamesWithEnabledValue(Boolean enabled)
			throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_WITH_ENABLED_VALUE, 
					new Object[] { enabled }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_WITH_ENABLED_VALUE +
						"' with parameter: " +
						enabled,
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesWithNewAccountValue(java.lang.Boolean)
	 */
	@Override
	public List<String> getUsernamesWithNewAccountValue(Boolean newAccount)
			throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_WITH_NEW_ACCOUNT_VALUE, 
					new Object[] { newAccount }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_WITH_NEW_ACCOUNT_VALUE +
						"' with parameter: " +
						newAccount,
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesWithCampaignCreationPrivilege(java.lang.Boolean)
	 */
	@Override
	public List<String> getUsernamesWithCampaignCreationPrivilege(
			Boolean campaignCreationPrivilege) throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_WITH_CAMPAIGN_CREATION_PRIVILEGE, 
					new Object[] { campaignCreationPrivilege }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_WITH_CAMPAIGN_CREATION_PRIVILEGE +
						"' with parameter: " +
						campaignCreationPrivilege,
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesFromPartialFirstName(java.lang.String)
	 */
	@Override
	public List<String> getUsernamesFromPartialFirstName(String partialFirstName)
			throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_LIKE_FIRST_NAME, 
					new Object[] { "%" + partialFirstName + "%" }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_LIKE_FIRST_NAME +
						"' with parameter: " +
						"%" + partialFirstName + "%",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesFromPartialLastName(java.lang.String)
	 */
	@Override
	public List<String> getUsernamesFromPartialLastName(String partialLastName)
			throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_LIKE_LAST_NAME, 
					new Object[] { "%" + partialLastName + "%" }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_LIKE_LAST_NAME +
						"' with parameter: " +
						"%" + partialLastName + "%",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesFromPartialOrganization(java.lang.String)
	 */
	@Override
	public List<String> getUsernamesFromPartialOrganization(
			String partialOrganization) throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_LIKE_ORGANIZATION, 
					new Object[] { "%" + partialOrganization + "%" }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_LIKE_ORGANIZATION +
						"' with parameter: " +
						"%" + partialOrganization + "%",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesFromPartialPersonalId(java.lang.String)
	 */
	@Override
	public List<String> getUsernamesFromPartialPersonalId(
			String partialPersonalId) throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_LIKE_PERSONAL_ID, 
					new Object[] { "%" + partialPersonalId + "%" }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_LIKE_PERSONAL_ID +
						"' with parameter: " +
						"%" + partialPersonalId + "%",
					e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUsernamesFromPartialEmailAddress(java.lang.String)
	 */
	@Override
	public List<String> getUsernamesFromPartialEmailAddress(
			String partialEmailAddress) throws DataAccessException {

		try {
			return getJdbcTemplate().query(
					SQL_GET_USERNAMES_LIKE_EMAIL_ADDRESS, 
					new Object[] { "%" + partialEmailAddress + "%" }, 
					new SingleColumnRowMapper<String>()
				);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing SQL '" +
						SQL_GET_USERNAMES_LIKE_EMAIL_ADDRESS +
						"' with parameter: " +
						"%" + partialEmailAddress + "%",
					e);
		}
	}
	
	/**
	 * Retrieves the personal information for a user or null if the user 
	 * doesn't have any personal information.
	 *
	 * @param username The username of the user whose information is being
	 * 				   retrieved.
	 * 
	 * @return If the user has a personal entry in the database, a UserPersonal
	 * 		   object with that information is returned; otherwise, null is
	 * 		   returned.
	 * 
	 * @throws DataAccessException Thrown if there is an error.
	 */
	public UserPersonal getPersonalInfoForUser(String username) throws DataAccessException {
		try {
			return getJdbcTemplate().queryForObject(
					SQL_GET_USER_PERSONAL, 
					new Object[] { username }, 
					new RowMapper<UserPersonal>() {
						@Override
						public UserPersonal mapRow(
								final ResultSet rs, 
								final int rowNum) 
								throws SQLException {
							
							try {
								return new UserPersonal(
										rs.getString("first_name"),
										rs.getString("last_name"),
										rs.getString("organization"),
										rs.getString("personal_id"));
							} 
							catch(DomainException e) {
								throw new SQLException(
										"Error creating the user's personal information.",
										e);
							}
						}
					});
		}
		catch(org.springframework.dao.IncorrectResultSizeDataAccessException e) {
			if(e.getActualSize() > 1) {
				throw new DataAccessException("There are multiple users with the same username.", e);
			}
			
			return null;
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException("Error executing the following SQL '" + SQL_GET_USER_PERSONAL + "' with parameter: " + username, e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#getUserInformation(java.util.Collection, java.lang.String, java.lang.String, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, long, long)
	 */
	public QueryResult<UserInformation> getUserInformation(
			final Collection<String> usernames,
			final String likeUsername,
			final String emailAddress,
			final Boolean admin,
			final Boolean enabled,
			final Boolean newAccount,
			final Boolean canCreateCampaigns,
			final String firstName,
			final String lastName,
			final String organization,
			final String personalId,
			final boolean like,
			final long numToSkip,
			final long numToReturn)
			throws DataAccessException {
		
		// The initial SELECT selects everything.
		StringBuilder sql = 
				new StringBuilder(
						"SELECT u.username, " +
							"u.email_address, " +
							"u.admin, " +
							"u.enabled, " +
							"u.new_account, " +
							"u.campaign_creation_privilege, " +
							"up.first_name, " +
							"up.last_name, " +
							"up.organization, " +
							"up.personal_id " +
						"FROM user u " +
						"LEFT JOIN user_personal up ON " +
							"u.id = up.user_id");
		
		// The initial parameter list doesn't have any items.
		Collection<Object> parameters = new LinkedList<Object>();
		
		// The initial WHERE clause with a flag to indicate if any components
		// of the WHERE clause had been added.
		boolean whereClauseNeeded = false;
		StringBuilder whereClause = new StringBuilder(" WHERE");
		
		// If the list of usernames is present, add a WHERE clause component
		// that limits the results to only those users whose exact username is
		// in the list.
		if(usernames != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("u.username IN ")
				.append(StringUtils.generateStatementPList(usernames.size()));
			
			parameters.addAll(usernames);
			
			whereClauseNeeded = true;
		}
		
		// If the "like username" value is present, add a WHERE clause 
		// component that limits the results to only those users whose username
		// is LIKE this string.
		if(likeUsername != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("u.username LIKE ?");
			
			parameters.add("%" + likeUsername + "%");
			
			whereClauseNeeded = true;
		}
		
		// If "emailAddress" is present, add a WHERE clause component that  
		// limits the results to only those whose email address either contains  
		// or exactly matches this value based on the 'like' parameter.
		if(emailAddress != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("up.email_address ")
				.append((like) ? " LIKE ?" : " = ?");
			
			if(like) {
				parameters.add("%" + emailAddress + "%");
			}
			else {
				parameters.add(emailAddress);
			}
			
			whereClauseNeeded = true;
		}
		
		// If "admin" is present, add a WHERE clause component that limits the
		// results to only those whose admin boolean is the same as this 
		// boolean.
		if(admin != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("u.admin = ?");
			
			parameters.add(admin);
			
			whereClauseNeeded = true;
		}
		
		// If "enabled" is present, add a WHERE clause component that limits 
		// the results to only those whose enabled value is the same as this
		// boolean
		if(enabled != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("u.enabled = ?");
			
			parameters.add(enabled);
			
			whereClauseNeeded = true;
		}
		
		// If "newAccount" is present, add a WHERE clause component that limits
		// the results to only those whose new account status is the same as
		// this boolean.
		if(newAccount != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("u.new_account = ?");
			
			parameters.add(newAccount);
			
			whereClauseNeeded = true;
		}
		
		// If "canCreateCampaigns" is present, add a WHERE clause component 
		// that limits the results to only those whose campaign creation
		// privilege is the same as this boolean.
		if(canCreateCampaigns != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("u.campaign_creation_privilege = ?");
			
			parameters.add(canCreateCampaigns);
			
			whereClauseNeeded = true;
		}
		
		// If "firstName" is present, add a WHERE clause component that limits
		// the results to only those whose first name either contains or 
		// exactly matches this value based on the 'like' parameter.
		if(firstName != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("up.first_name ")
				.append((like) ? " LIKE ?" : " = ?");
			
			if(like) {
				parameters.add("%" + firstName + "%");
			}
			else {
				parameters.add(firstName);
			}
			
			whereClauseNeeded = true;
		}
		
		// If "lastName" is present, add a WHERE clause component that limits
		// the results to only those whose last name either contains or exactly
		// matches this value based on the 'like' parameter.
		if(lastName != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("up.last_name ")
				.append((like) ? " LIKE ?" : " = ?");
			
			if(like) {
				parameters.add("%" + lastName + "%");
			}
			else {
				parameters.add(lastName);
			}
			
			whereClauseNeeded = true;
		}
		
		// If "organization" is present, add a WHERE clause component that 
		// limits the results to only those whose organization either contains 
		// or exactly matches this value based on the 'like' parameter.
		if(organization != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("up.organization ")
				.append((like) ? " LIKE ?" : " = ?");
			
			if(like) {
				parameters.add("%" + organization + "%");
			}
			else {
				parameters.add(organization);
			}
			
			whereClauseNeeded = true;
		}
		
		// If "personalId" is present, add a WHERE clause component that limits 
		// the results to only those whose personal ID either contains or 
		// exactly matches this value based on the 'like' parameter.
		if(personalId != null) {
			whereClause
				.append((whereClauseNeeded) ? " AND " : " ")
				.append("up.personal_id ")
				.append((like) ? " LIKE ?" : " = ?");
			
			if(like) {
				parameters.add("%" + personalId + "%");
			}
			else {
				parameters.add(personalId);
			}
			
			whereClauseNeeded = true;
		}
		
		// Finally, add the WHERE clause to the SQL if any components were 
		// added.
		if(whereClauseNeeded) {
			sql.append(whereClause);
		}
		
		// Always order the results by username to facilitate paging.
		sql.append(" ORDER BY u.username");
		
		// Returns the results as queried by the database.
		try {
			return getJdbcTemplate().query(
					sql.toString(), 
					parameters.toArray(),
					new ResultSetExtractor<QueryResult<UserInformation>>() {
						/**
						 * Extracts the data into the results and then returns
						 * the total number of results found.
						 */
						@Override
						public QueryResult<UserInformation> extractData(
								final ResultSet rs)
								throws SQLException,
								org.springframework.dao.DataAccessException {
							
							try {
								QueryResultBuilder<UserInformation> builder =
										new QueryResultBuilder<UserInformation>();
								
								int numSkipped = 0;
								while(numSkipped++ < numToSkip) {
									if(rs.next()) {
										builder.increaseTotalNumResults();
									}
									else {
										return builder.getQueryResult();
									}
								}
								
								long numReturned = 0;
								while(numReturned++ < numToReturn) {
									if(rs.next()) {
										builder.addResult(mapRow(rs));
									}
									else {
										return builder.getQueryResult();
									}
								}
								
								while(rs.next()) {
									builder.increaseTotalNumResults();
								}
								
								return builder.getQueryResult();
							}
							catch(DomainException e) {
								throw new org.springframework.dao.DataIntegrityViolationException(
										"There was an error building the result.",
										e);
							}
						}
						
						/**
						 * Creates a new UserInformation object from the 
						 * user information.
						 */
						private UserInformation mapRow(
								final ResultSet rs)
								throws SQLException {
							
							String username = rs.getString("username");
							String emailAddress = 
									rs.getString("email_address");
							
							boolean admin = rs.getBoolean("admin");
							boolean enabled = rs.getBoolean("enabled");
							boolean newAccount = rs.getBoolean("new_account");
							boolean canCreateCampaigns =
									rs.getBoolean(
											"campaign_creation_privilege");
							
							String firstName = rs.getString("first_name");
							String lastName = rs.getString("last_name");
							String organization = rs.getString("organization");
							String personalId = rs.getString("personal_id");
							
							UserPersonal personalInfo = null;
							if((firstName != null) &&
									(lastName != null) &&
									(organization != null) &&
									(personalId != null)) {
								
								try {
									personalInfo = new UserPersonal(
											firstName,
											lastName,
											organization,
											personalId);
								} 
								catch(DomainException e) {
									throw new SQLException(
											"Error creating the user's personal information.",
											e);
								}
							}
							
							try {
								return new UserInformation(
										username,
										emailAddress,
										admin,
										enabled,
										newAccount,
										canCreateCampaigns,
										Collections.
											<String, Set<Campaign.Role>>
												emptyMap(),
										Collections.
											<String, Clazz.Role>
												emptyMap(),
										personalInfo);
							}
							catch(DomainException e) {
								throw new SQLException(
										"Error creating the user's information.",
										e);
							}
						}
					}
			);
		}
		catch(org.springframework.dao.DataAccessException e) {
			throw new DataAccessException(
					"Error executing the following SQL '" + 
						sql.toString() + 
						"' with parameter(s): " + 
						parameters);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ohmage.query.IUserQueries#updateUser(java.lang.String, java.lang.String, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void updateUser(
			final String username, 
			final String emailAddress,
			final Boolean admin, 
			final Boolean enabled, 
			final Boolean newAccount, 
			final Boolean campaignCreationPrivilege,
			final String firstName,
			final String lastName,
			final String organization,
			final String personalId) 
			throws DataAccessException {
		
		// Create the transaction.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Updating a user's privileges and information.");
		
		try {
			// Begin the transaction.
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def);
			
			if(emailAddress != null) {
				try {
					getJdbcTemplate().update(SQL_UPDATE_EMAIL_ADDRESS, emailAddress, username);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_EMAIL_ADDRESS + "' with parameters: " + 
							emailAddress + ", " + username, e);
				}
			}
			
			// Update the admin value if it's not null.
			if(admin != null) {
				try {
					getJdbcTemplate().update(SQL_UPDATE_ADMIN, admin, username);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_ADMIN + "' with parameters: " + 
							admin + ", " + username, e);
				}
			}
			
			// Update the enabled value if it's not null.
			if(enabled != null) {
				try {
					getJdbcTemplate().update(SQL_UPDATE_ENABLED, enabled, username);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_ENABLED + "' with parameters: " + 
							enabled + ", " + username, e);
				}
			}
			
			// Update the new account value if it's not null.
			if(newAccount != null) {
				try {
					getJdbcTemplate().update(SQL_UPDATE_NEW_ACCOUNT, newAccount, username);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_NEW_ACCOUNT + "' with parameters: " + 
							newAccount + ", " + username, e);
				}
			}
			
			// Update the campaign creation privilege value if it's not null.
			if(campaignCreationPrivilege != null) {
				try {
					getJdbcTemplate().update(SQL_UPDATE_CAMPAIGN_CREATION_PRIVILEGE, campaignCreationPrivilege, username);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_CAMPAIGN_CREATION_PRIVILEGE + "' with parameters: " + 
							campaignCreationPrivilege + ", " + username, e);
				}
			}
			
			if(userHasPersonalInfo(username)) {
				if(firstName != null) {
					try {
						getJdbcTemplate().update(SQL_UPDATE_FIRST_NAME, firstName, username);
					}
					catch(org.springframework.dao.DataAccessException e) {
						transactionManager.rollback(status);
						throw new DataAccessException("Error executing SQL '" + SQL_UPDATE_FIRST_NAME + "' with parameters: " +
								firstName + ", " + username, e);
					}
				}
				
				if(lastName != null) {
					try {
						getJdbcTemplate().update(SQL_UPDATE_LAST_NAME, lastName, username);
					}
					catch(org.springframework.dao.DataAccessException e) {
						transactionManager.rollback(status);
						throw new DataAccessException("Error executing SQL '" + SQL_UPDATE_LAST_NAME + "' with parameters: " +
								lastName + ", " + username, e);
					}
				}
				
				if(organization != null) {
					try {
						getJdbcTemplate().update(SQL_UPDATE_ORGANIZATION, organization, username);
					}
					catch(org.springframework.dao.DataAccessException e) {
						transactionManager.rollback(status);
						throw new DataAccessException("Error executing SQL '" + SQL_UPDATE_ORGANIZATION + "' with parameters: " +
								organization + ", " + username, e);
					}
				}
				
				if(personalId != null) {
					try {
						getJdbcTemplate().update(SQL_UPDATE_PERSONAL_ID, personalId, username);
					}
					catch(org.springframework.dao.DataAccessException e) {
						transactionManager.rollback(status);
						throw new DataAccessException("Error executing SQL '" + SQL_UPDATE_PERSONAL_ID + "' with parameters: " +
								personalId + ", " + username, e);
					}
				}
			}
			else {
				try {
					getJdbcTemplate().update(
							SQL_INSERT_USER_PERSONAL, 
							username, 
							firstName, 
							lastName, 
							organization, 
							personalId);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException(
							"Error executing SQL '" + SQL_INSERT_USER_PERSONAL + "' with parameters: " +
								username + ", " + 
								firstName + ", " + 
								lastName + ", " + 
								organization + ", " + 
								personalId, 
							e);
				}
			}
			
			// Commit the transaction.
			try {
				transactionManager.commit(status);
			}
			catch(TransactionException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while committing the transaction.", e);
			}
		}
		catch(TransactionException e) {
			throw new DataAccessException("Error while attempting to rollback the transaction.", e);
		}
	}
	
	/**
	 * Updates a user's password.
	 * 
	 * @param username The username of the user to be updated.
	 * 
	 * @param hashedPassword The new, hashed password for the user.
	 */
	public void updateUserPassword(String username, String hashedPassword) throws DataAccessException {
		// Create the transaction.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Updating a user's password.");
		
		try {
			// Begin the transaction.
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def);
			
			// Update the password.
			try {
				getJdbcTemplate().update(SQL_UPDATE_PASSWORD, hashedPassword, username);
			}
			catch(org.springframework.dao.DataAccessException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_PASSWORD + "' with parameters: " + 
						hashedPassword + ", " + username, e);
			}
			
			// Ensure that this user is no longer a new user.
			try {
				getJdbcTemplate().update(SQL_UPDATE_NEW_ACCOUNT, false, username);
			}
			catch(org.springframework.dao.DataAccessException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_NEW_ACCOUNT + "' with parameters: " + 
						false + ", " + username, e);
			}
			
			// Commit the transaction.
			try {
				transactionManager.commit(status);
			}
			catch(TransactionException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while committing the transaction.", e);
			}
		}
		catch(TransactionException e) {
			throw new DataAccessException("Error while attempting to rollback the transaction.", e);
		}
	}
	
	/**
	 * Deletes all of the users in a Collection.
	 * 
	 * @param usernames A Collection of usernames for the users to delete.
	 */
	public void deleteUsers(Collection<String> usernames) throws DataAccessException {
		// Create the transaction.
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		def.setName("Deleting a user.");
		
		try {
			// Begin the transaction.
			PlatformTransactionManager transactionManager = new DataSourceTransactionManager(getDataSource());
			TransactionStatus status = transactionManager.getTransaction(def);
			
			// Delete the users.
			for(String username : usernames) {
				try {
					getJdbcTemplate().update(SQL_DELETE_USER, username);
				}
				catch(org.springframework.dao.DataAccessException e) {
					transactionManager.rollback(status);
					throw new DataAccessException("Error executing the following SQL '" + SQL_UPDATE_PASSWORD + "' with parameters: " + 
							username, e);
				}
			}
			
			// Commit the transaction.
			try {
				transactionManager.commit(status);
			}
			catch(TransactionException e) {
				transactionManager.rollback(status);
				throw new DataAccessException("Error while committing the transaction.", e);
			}
		}
		catch(TransactionException e) {
			throw new DataAccessException("Error while attempting to rollback the transaction.", e);
		}
	}
}