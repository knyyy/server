package edu.ucla.cens.awserver.service;

import edu.ucla.cens.awserver.cache.ConfigurationCacheService;
import edu.ucla.cens.awserver.domain.Configuration;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.RetrieveConfigAwRequest;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * Dispatches to the data layer to retrieve configurations for the campaigns the User in the AwRequest belongs to.
 * 
 * @author selsky
 */
public class ConfigurationRetrievalService extends AbstractAnnotatingService {
	private ConfigurationCacheService _configCacheService;
	
	public ConfigurationRetrievalService(ConfigurationCacheService cacheService, AwRequestAnnotator annotator) {
		super(annotator);
		
		if(null == cacheService) {
			throw new IllegalArgumentException("a ConfigurationCacheService is required");
		}
		_configCacheService = cacheService;
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		// Currently only one configuration is allowed per campaign -- multiple versions of a campaign are disallowed 
		// to avoid complexity on the UI side with having to handle data points (promptIds) across multiple configuration
		// versions
		Configuration c = _configCacheService.lookupByCampaign(awRequest.getCampaignName());
		
		((RetrieveConfigAwRequest) awRequest).setOutputConfigXml(c.getXml());
	}
}
