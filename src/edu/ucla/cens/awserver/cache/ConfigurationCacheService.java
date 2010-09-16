package edu.ucla.cens.awserver.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.dao.ParameterLessDao;
import edu.ucla.cens.awserver.domain.CampaignNameVersion;
import edu.ucla.cens.awserver.domain.Configuration;

/**
 * The default implementation of a ConfigurationCacheService. Contains Configurations accessible by campaign name-version pairs.
 * 
 * @author selsky
 */
public class ConfigurationCacheService extends AbstractCacheService {
	private static Logger _logger = Logger.getLogger(ConfigurationCacheService.class);
	private Map<CampaignNameVersion, Configuration> _configurationMap;
	
	/**
	 * The provided DAO will be used to load the cache.
	 */
	public ConfigurationCacheService(ParameterLessDao dao) {
		super(dao);
		init();
	}
	
	/**
	 * Loads all campaign configurations into a cache for in-memory querying.
	 */
	private void init() {
		List<Configuration> configurations = (List<Configuration>) _dao.execute();
		
		if(configurations.size() < 1) {
			throw new IllegalStateException("cannot startup with zero configurations");
		}
		
		_logger.info("loaded " + configurations.size() + " campaign configurations");
		
//		for(Configuration c : _configurations) {
//			_logger.info(c);
//		}
		
		_configurationMap = new HashMap<CampaignNameVersion, Configuration>();
		
		for(Configuration c : configurations) {
			CampaignNameVersion cnv = new CampaignNameVersion(c.getCampaignName(), c.getCampaignVersion());
			_configurationMap.put(cnv, c);
		}
	}

	/**
	 * Returns a Configuration given a CampaignNameVersion key.
	 */
	@Override
	public Object lookup(Object key) {
		return _configurationMap.get(key);
	}
	
	/**
	 * Returns whether this cache contains a Configuration identified by the provided key.
	 */
	@Override
	public boolean containsKey(Object key) {
		return _configurationMap.containsKey(key);
	}
}
