package edu.ucla.cens.awserver.cache;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.dao.ParameterLessDao;
import edu.ucla.cens.awserver.domain.PromptType;

/**
 * Simple cache for prompt types that allows lookup of the database primary key for the prompt_type table based on the text label 
 * of the prompt type. This class does not use synchronization as it is assumed that once the prompt types are loaded,
 * they will not change. 
 * 
 * TODO: this class is nearly identical to UserRoleCacheService
 * TODO: add reload functionality (and synchronization) (new interface: ReloadableCacheService)
 * 
 * @author selsky
 */
public class PromptTypeCacheService extends AbstractCacheService {
	private Map<String, Integer> _cache;
	private static final Logger _logger = Logger.getLogger(PromptTypeCacheService.class);
		
	/**
	 * Initialize the local cache using the provided Dao.
	 * 
	 * @throws IllegalArgumentException if the provided Dao is null
	 */
	public PromptTypeCacheService(ParameterLessDao dao) {
		super(dao);
		init();
	}
	
	/**
	 * @throws IllegalStateException if no prompt types are found in the database
	 */
	private void init() {
		_cache = new TreeMap<String, Integer> ();
		
		List<?> promptTypes = _dao.execute();
		
		int listSize = promptTypes.size();
		if(listSize < 1) {
			throw new IllegalStateException("database has incorrect state - no prompt types found");
		}
		
		for(int i = 0; i < listSize; i++) {
			PromptType pt = (PromptType) promptTypes.get(i);
			_cache.put(pt.getType(), pt.getId());
		}
		
		_logger.info("prompt type cache loaded with " + listSize + " types");
	}
	
	/**
	 * @return the Integer primary key of the prompt type defined by the provided text.
	 */
	@Override
	public Object lookup(Object key) {
		return _cache.get(key); // no copy because Strings are immutable
	}
}
