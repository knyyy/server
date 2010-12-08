package edu.ucla.cens.awserver.service;

import java.util.List;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.MediaQueryAwRequest;
import edu.ucla.cens.awserver.validator.AwRequestAnnotator;

/**
 * @author selsky
 */
public class FindUrlForMediaIdService extends AbstractDaoService {
	private static Logger _logger = Logger.getLogger(FindUrlForMediaIdService.class);
	private AwRequestAnnotator _noMediaAnnotator;
	private AwRequestAnnotator _severeAnnotator;
	
	public FindUrlForMediaIdService(Dao dao, AwRequestAnnotator noMediaAnnotator, AwRequestAnnotator severeAnnotator) {
		super(dao);
		
		if(null == noMediaAnnotator) {
			throw new IllegalArgumentException("noMediaAnnotator cannot be null");
		}
		if(null == severeAnnotator) {
			throw new IllegalArgumentException("severeAnnotator cannot be null");
		}
		
		_noMediaAnnotator = noMediaAnnotator;
		_severeAnnotator = severeAnnotator;
	}
	
	@Override
	public void execute(AwRequest awRequest) {
		getDao().execute(awRequest);
		List<?> results = awRequest.getResultList();
		
		if(0 == results.size()) {
			
			_noMediaAnnotator.annotate(awRequest, "no media url found for id");
			
		} else if (1 == results.size()){
			
			((MediaQueryAwRequest) awRequest).setMediaUrl((String) results.get(0));
			
		} else { // bad! the media id is supposed to be a unique key
			
			_logger.error("more than one url found for media id " + ((MediaQueryAwRequest) awRequest).getMediaId());
			_severeAnnotator.annotate(awRequest, "more than one url found for media id");
			
		}
	}
}
