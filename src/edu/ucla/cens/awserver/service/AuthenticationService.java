package edu.ucla.cens.awserver.service;

import java.util.List;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.dao.DataAccessException;
import edu.ucla.cens.awserver.dao.LoginResult;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.StringUtils;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * Service for authenticating users via phone or web page.
 * 
 * @author selsky
 */
public class AuthenticationService extends AbstractAnnotatingDaoService {
	private static Logger _logger = Logger.getLogger(AuthenticationService.class);
	private String _errorMessage;
	private String _disabledMessage;
	private String _newAccountMessage;
	private boolean _newAccountsAllowed;
	
	/**
	 * Creates an instance of this class using the supplied DAO as the method of data access. The *Message parameters are set 
	 * through this constructor in order to customize the output depending on the context in which this component is used  
	 * (i.e., is login occuring through a web page or a phone). The newAccountsAllowed parameter specifies whether this 
	 * service will treat new accounts as successful logins. New accounts are allowed access for the intial login via a phone and
	 * blocked in every other case.
	 * 
	 * @throws IllegalArgumentException if errorMessage is null, empty, or all whitespace 
	 * @throws IllegalArgumentException if disabledMessage is null, empty, or all whitespace
	 */
	public AuthenticationService(Dao dao, AwRequestAnnotator awRequestAnnotator, String errorMessage, String disabledMessage, 
		String newAccountMessage, boolean newAccountsAllowed) {
		
		super(dao, awRequestAnnotator);
		if(StringUtils.isEmptyOrWhitespaceOnly(errorMessage)) {
			throw new IllegalArgumentException("an error message is required");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(disabledMessage)) {
			throw new IllegalArgumentException("a disabled message is required");
		}
		if(StringUtils.isEmptyOrWhitespaceOnly(newAccountMessage)) {
			_logger.info("configured without a new account message");
		}
		
		_errorMessage = errorMessage;
		_disabledMessage = disabledMessage;
		_newAccountMessage = newAccountMessage;
		_newAccountsAllowed = newAccountsAllowed;
	}
	
	/**
	 * If a user is found by the DAO, sets the id and the campaign id on the User in the AwRequest. If a user is not found, sets 
	 * the failedRequest and failedRequestErrorMessage properties on the AwRequest.
	 * 
	 * @throws ServiceException if any DataAccessException occurs in the data layer
	 */
	public void execute(AwRequest awRequest) {
		try {
			// It would be nice if the service could tell the DAO how to format
			// the results the DAO returns
			getDao().execute(awRequest);
			
			List<?> results = awRequest.getResultList();
			
			if(null != results && results.size() > 0) {
				
				for(int i = 0; i < results.size(); i++) {
				
					LoginResult loginResult = (LoginResult) results.get(i);
					
					if(! _newAccountsAllowed && loginResult.isNew()) {
						
						getAnnotator().annotate(awRequest, _newAccountMessage);
						_logger.info("user " + awRequest.getUser().getUserName() + " is new and must change their password via " +
							"phone before being granted access");
						
						return;
					}
					
					if(! loginResult.isEnabled()) {
						
						getAnnotator().annotate(awRequest, _disabledMessage);
						_logger.info("user " + awRequest.getUser().getUserName() + " is not enabled for access");
						
						return;
						
					}
					
					if(0 == i) { // first time thru: grab the properties that are common across all LoginResults (i.e., data from
						         // the user table)
						
						awRequest.getUser().setId(loginResult.getUserId());
						awRequest.getUser().setLoggedIn(true);
					}
					
					// set the campaigns and the roles within the campaigns that the user belongs to
					awRequest.getUser().addCampaignRole(loginResult.getCampaignId(), loginResult.getUserRoleId());
				}
				
				// Set the current campaign on the user if the user belongs to only one campaign. If the user belongs to more
				// than one campaign, he or she will have to choose a campaign post-login.
				if(userBelongsToOneCampaign(results)) {
					awRequest.getUser().setCurrentCampaignId(((LoginResult)results.get(0)).getCampaignId());
				}
				
				
				_logger.info("user " + awRequest.getUser().getUserName() + " successfully logged in");
				
			} else { // no user found or invalid password
				
				getAnnotator().annotate(awRequest, _errorMessage);
				_logger.info("user " + awRequest.getUser().getUserName() + " not found or invalid password was supplied");
			}
			
		} catch (DataAccessException dae) { 
			
			throw new ServiceException(dae);
		}
	}
	
	/**
	 * Check to see whether the results list contains multiple campaigns. 
	 */
	private boolean userBelongsToOneCampaign(List<?> results) {
		if(results.size() == 1) {
			return true;
		} else {
			int cid = ((LoginResult) results.get(0)).getCampaignId();
			for(int i = 1; i < results.size(); i++) {
				if(cid != ((LoginResult) results.get(i)).getCampaignId()) {
					return false;
				}
			}
		}
		return true;
	}
}
