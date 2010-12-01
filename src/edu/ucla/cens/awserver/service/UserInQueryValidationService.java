package edu.ucla.cens.awserver.service;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.cache.CacheService;
import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * Perform a sanity check against a user parameter present as part of a query: make sure that the user represented by the 
 * authentication token has access to the campaign specified in the query; that the user in
 * the query (the u parameter) belongs to the campaign specified in the query; and that the logged in user and the query user are 
 * the same if the logged in user is not a researcher or admin.
 * 
 * TODO - split apart into separate classes because the execute method does too much and contains functionality that could be 
 * reused elsewhere
 * 
 * @author selsky
 */
public class UserInQueryValidationService extends AbstractDaoService {
	private static Logger _logger = Logger.getLogger(UserInQueryValidationService.class);
	
	private AwRequestAnnotator _invalidCampaignAnnotator;
	private AwRequestAnnotator _invalidUserAnnotator;
	private CacheService _userRoleCacheService;
	
	public UserInQueryValidationService(Dao dao, CacheService userRoleCacheService,
		AwRequestAnnotator invalidCampaignAnnotator, AwRequestAnnotator invalidUserAnnotator) {
		
		super(dao);
		if(null == invalidCampaignAnnotator) {
			throw new IllegalArgumentException("an invalid campaign annotator is required");
		}
		if(null == invalidUserAnnotator) {
			throw new IllegalArgumentException("an invalid user annotator is required");
		}
		if(null == userRoleCacheService) {
			throw new IllegalArgumentException("a user role cache service is required");
		}
		
		_userRoleCacheService = userRoleCacheService;
		_invalidUserAnnotator = invalidUserAnnotator;
		_invalidCampaignAnnotator = invalidCampaignAnnotator;
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		
		// check whether the logged in user has access to the campaign in the query params
		
		Set<String> allowedCampaigns = awRequest.getUser().getCampaignRoles().keySet();
		
		if(! allowedCampaigns.contains(awRequest.getCampaignName())) {
			_logger.warn("user attempting to query against a campaign they do not belong to. user: " + 
				awRequest.getUser().getUserName() + " campaign: " + awRequest.getCampaignName());
			_invalidCampaignAnnotator.annotate(awRequest, "user attempt to query a campaign they do not belong to");
			return;
		}
		
		// check whether a non-researcher or non-admin user is attempting to run queries for other users
		
		// this code is duplicated in ConfigurationRetrievalService. it's a pretty common task that could
		// be outsourced to another class
		List<Integer> list = awRequest.getUser().getCampaignRoles().get(awRequest.getCampaignName());
		boolean isAdminOrResearcher = false;
		
		for(Integer i : list) {
			String role = (String) _userRoleCacheService.lookup(i);
			
			if("researcher".equals(role) || "admin".equals(role)) {
				isAdminOrResearcher = true;
				break;
			}
		}
		
		if(! isAdminOrResearcher) { // participants can only run queries for themselves
			
			if(! awRequest.getUser().getUserName().equals(awRequest.getUserNameRequestParam())) {
				_logger.warn("logged in participant attempting to run query for another user. " 
					+ " logged in user: " +  awRequest.getUser().getUserName() + " query user: "
					+ awRequest.getUserNameRequestParam());
				_invalidUserAnnotator.annotate(awRequest, "logged in user and query user must be the same for users with a role "  
					+ "of participant");
			}
			
		} else { // make sure the user query param represents a user that belongs to the campaign query param
			
			getDao().execute(awRequest);
			
			List<?> results = awRequest.getResultList();
			if(! results.contains(awRequest.getCampaignName())) {
				_logger.warn("logged in user attempting to query against a user who does not belong to the same campaigns. " 
					+ " logged in user: " +  awRequest.getUser().getUserName() + " query user: " 
					+ awRequest.getUserNameRequestParam() + " query campaign: " + awRequest.getCampaignName());
				_invalidUserAnnotator.annotate(awRequest, "logged in user and query user do not belong to the same campaigns");
			}
		}
	}
}
