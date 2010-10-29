package edu.ucla.cens.awserver.service;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.cache.ConfigurationCacheService;
import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.domain.CampaignNameVersion;
import edu.ucla.cens.awserver.domain.Configuration;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.DataPointQueryAwRequest;

/**
 * @author selsky
 */
public class DataPointQueryService extends AbstractDaoService {
	private static Logger _logger = Logger.getLogger(DataPointQueryService.class);
	private ConfigurationCacheService _configurationCacheService;
	
	public DataPointQueryService(Dao dao, ConfigurationCacheService configurationCacheService) {
		super(dao);
		if(null == configurationCacheService) {
			throw new IllegalArgumentException("a CacheService is required");
		}
		_configurationCacheService = configurationCacheService;
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		DataPointQueryAwRequest req = (DataPointQueryAwRequest) awRequest;
		
		// 1. Retrieve metadata data points from the config cache for the survey that wraps the prompt id being queried
		// Metadata data points are sent with each data point from a particular survey.
		
		CampaignNameVersion cnv = new CampaignNameVersion(req.getCampaignName(), req.getCampaignVersion());
		
		// TODO - what if the end user has selected a metadata data point? it means there will be redundancy between the 
		// dataPointId and the metadataPromptIds
		
		req.setMetadataPromptIds(((Configuration) _configurationCacheService.lookup(cnv)).getMetadataPromptIds(req.getDataPointId()));	
		
		// 2. Pass those ids to the DAO 
		// TODO - need a researcher version of the DAO
		getDao().execute(req);
		
		// 3. Post-process??
		_logger.info(req.getResultList());
	}
}
