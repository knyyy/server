package edu.ucla.cens.awserver.service;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * The Data Point API is very open so this class performs a data sanity check against the inbound query parameters. It makes 
 * sure that the user represented by the authentication token has access to the campaign specified in the query. It also makes
 * sure that the user in the query (the u parameter) belongs to the campaign specified in the query.
 * 
 * @author selsky
 */
public class DataPointQueryUserValidationService extends AbstractDaoService {
	private static Logger _logger = Logger.getLogger(DataPointQueryUserValidationService.class);
	
	private AwRequestAnnotator _invalidCampaignAnnotator;
	private AwRequestAnnotator _invalidUserAnnotator;
	
	public DataPointQueryUserValidationService(Dao dao, 
		AwRequestAnnotator invalidCampaignAnnotator, AwRequestAnnotator invalidUserAnnotator) {
		
		super(dao);
		if(null == invalidCampaignAnnotator) {
			throw new IllegalArgumentException("an invalid campaign annotator is required");
		}
		if(null == invalidUserAnnotator) {
			throw new IllegalArgumentException("an invalid user annotator is required");
		}
		
		_invalidUserAnnotator = invalidUserAnnotator;
		_invalidCampaignAnnotator = invalidCampaignAnnotator;
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		
		// make sure the logged in user has access to the campaign in the query params
		
		Set<String> allowedCampaigns = awRequest.getUser().getCampaignRoles().keySet();
		
		if(! allowedCampaigns.contains(awRequest.getCampaignName())) {
			_logger.warn("user attempting to query against a campaign they do not belong to. user: " + 
				awRequest.getUser().getUserName() + " campaign: " + awRequest.getCampaignName());
			_invalidCampaignAnnotator.annotate(awRequest, "user attempt to query a campaign they do not belong to");
			return;
		}
		
		// make sure the user query param represents a user that belongs to the campaign query param
		
		if(null != awRequest.getUserNameRequestParam()) { // if the logged in user is a researcher or admin, the user name param is
			                                              // not required. the role of the logged in user is not validated here.
			getDao().execute(awRequest);
			
			List<?> results = awRequest.getResultList();
			if(! results.contains(awRequest.getCampaignName())) {
				_logger.warn("logged in user attempting to query against a user who does not belong to the same set of campaigns." +
					" logged in user: " +  awRequest.getUser().getUserName() + " query user: " + awRequest.getUserNameRequestParam()
				    + " query campaign: " + awRequest.getCampaignName());
				_invalidUserAnnotator.annotate(awRequest, "logged in user and query user do not belong to the same campaigns");
			}
		}
	}
}
