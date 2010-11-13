package edu.ucla.cens.awserver.service;

import java.util.List;

import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.domain.MostRecentSurveyActivityQueryResult;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.DateUtils;

/**
 * Service that dispatches directly to a DAO without performing any pre- or post-processing.
 * 
 * @author selsky
 */
public class MostRecentSurveyQueryService implements Service {
	private Dao _dao;
	
	/**
     * @throws IllegalArgumentException if the provided Dao is null
     */
    public MostRecentSurveyQueryService(Dao dao) {
    	if(null == dao) {
    		throw new IllegalArgumentException("a DAO is required");    		
    	}
    	
    	_dao = dao;
    }
	
    /**
     * TODO document me
     */
	public void execute(AwRequest awRequest) {
		
		_dao.execute(awRequest);
		
		// Calculate the hours since the last update
		
		List<?> results = awRequest.getResultList();
		int size = results.size();
		
		for(int i = 0; i < size; i++) {
			
			MostRecentSurveyActivityQueryResult result = (MostRecentSurveyActivityQueryResult) results.get(i);
			
			if(result.getTimestamp() != null && result.getTimezone() != null) {
				long updateTime = result.getTimestamp().getTime() + DateUtils.systemTimezoneOffset(result.getTimezone());
				
				// Dates before the epoch will break this calculation 
				double difference = System.currentTimeMillis() - updateTime;
				
				// convert to hours
				result.setValue(((difference / 1000) / 60) / 60);
				
			} else {
				
				result.setValue(0d);
			}
		}
	}
}
