package edu.ucla.cens.awserver.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import edu.ucla.cens.awserver.cache.ConfigurationCacheService;
import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.domain.CampaignNameVersion;
import edu.ucla.cens.awserver.domain.Configuration;
import edu.ucla.cens.awserver.domain.DataPointQueryResult;
import edu.ucla.cens.awserver.domain.PromptTypeUtils;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.DataPointQueryAwRequest;
import edu.ucla.cens.awserver.util.JsonUtils;

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
		// dataPointId and the metadataPromptIds. not really the end of world, just sloppy.
		
		Configuration config = (Configuration) _configurationCacheService.lookup(cnv);
		List<String> metadataPromptIds = new ArrayList<String>();
		String[] dataPointIds = req.getDataPointIds();
		
		for(String dataPointId : dataPointIds) {
			List<String> list = config.getMetadataPromptIds(dataPointId); 
			if(! metadataPromptIds.containsAll(list)) {
				metadataPromptIds.addAll(list);
			}
		}
		
		req.setMetadataPromptIds(metadataPromptIds);	
		
		// 2. Pass those ids to the DAO
		getDao().execute(req);
		
		// 3. Post-process
		// Label the data points that are metadata
		// Set the displayLabel and the unit from the survey config
		List<?> results = req.getResultList();
		int numberOfResults = results.size();
		
		for(int i = 0; i < numberOfResults; i++) {
			DataPointQueryResult result = (DataPointQueryResult) results.get(i);
			
			if(metadataPromptIds.contains(result.getPromptId())) {
				result.setIsMetadata(true);
			}
			
			if(result.isRepeatableSetResult()) {
				
				result.setUnit(config.getUnitFor(result.getSurveyId(), result.getRepeatableSetId(), result.getPromptId()));
				result.setDisplayLabel(
					config.getDisplayLabelFor(result.getSurveyId(), result.getRepeatableSetId(), result.getPromptId())
				);
				result.setDisplayType(config.getDisplayTypeFor(
					result.getSurveyId(), result.getRepeatableSetId(), result.getPromptId())
				);
				
				if(PromptTypeUtils.isSingleChoiceType(result.getPromptType())) {
					
					String value = config.getValueForChoiceKey(
						result.getSurveyId(), result.getRepeatableSetId(), result.getPromptId(), String.valueOf(result.getResponse())
					);
					
					if(null != value) {
						
						Number number = toNumber(value);
						
						if(null != number) {
							result.setDisplayValue(number);
						} else {
							result.setDisplayValue(value);
						}
						
					} else { // just use the response if there is no display value
						
						result.setDisplayValue(toNumber(result.getResponse()));
					}
					
				} else if(PromptTypeUtils.isMultiChoiceType(result.getPromptType())) {
					
					JSONArray responseArray = JsonUtils.getJsonArrayFromString(String.valueOf(result.getResponse()));
					
					if(null == responseArray) { // very bad - this means we have invalid data in the db
						_logger.error("unparseable JSON array found for multi-choice response: " + result.getResponse());
						throw new ServiceException("unparseable JSONArray: " + result.getResponse());
					}
					
					JSONArray valueArray = new JSONArray();
					int length = responseArray.length();
					
					for(int j = 0; j < length; j++) {
						
						Object value = config.getValueForChoiceKey(result.getSurveyId(), result.getRepeatableSetId(), 
							result.getPromptId(), JsonUtils.getStringFromJsonArray(responseArray, j)
						);
						
						if(null == value) { // ok, get the label
//							Object label = config.getLabelForChoiceKey(result.getSurveyId(), result.getRepeatableSetId(), 
//								result.getPromptId(), JsonUtils.getStringFromJsonArray(responseArray, j)
//							);
//							
//							result.setDisplayValue(label);
							break;
							
						} else {
							
							valueArray.put(value);
						}
					}
					
					if(null == result.getDisplayValue()) {
						result.setDisplayValue(valueArray);
					}
					
				} else { 
					
					if(PromptTypeUtils.isNumberPromptType(result.getPromptType())) { // check for a number to avoid numbers being
						                                                             // quoted in the JSON output
						result.setDisplayValue(toNumber(result.getResponse())); 
						
					} else {
						
						result.setDisplayValue(result.getResponse());
					}
				}
				
			} else {
				
				result.setUnit(config.getUnitFor(result.getSurveyId(), result.getPromptId()));
				result.setDisplayLabel(config.getDisplayLabelFor(result.getSurveyId(), result.getPromptId()));
				result.setDisplayType(config.getDisplayTypeFor(result.getSurveyId(), result.getPromptId()));
				
				if(PromptTypeUtils.isSingleChoiceType(result.getPromptType())) {
				
					String value = config.getValueForChoiceKey(
						result.getSurveyId(), result.getPromptId(), String.valueOf(result.getResponse())
					);
					
					if(null != value) {
						
						Number number = toNumber(value);
						
						if(null != number) {
							result.setDisplayValue(number);
						} else {
							result.setDisplayValue(value);
						}
						
					} else {
						// single_choice response values are always integers based on our specification
						result.setDisplayValue(toNumber(result.getResponse()));
					}
				
				} else if (PromptTypeUtils.isMultiChoiceType(result.getPromptType())) {
					
					JSONArray responseArray = JsonUtils.getJsonArrayFromString(String.valueOf(result.getResponse()));
					
					if(null == responseArray) { // very bad - this means we have invalid data in the db
						throw new IllegalStateException("unparseable JSONArray: " + result.getResponse());
					}
					
					
					JSONArray valueArray = new JSONArray();
					int length = responseArray.length();
					
					for(int j = 0; j < length; j++) {
						
						Object value = config.getValueForChoiceKey(result.getSurveyId(), result.getRepeatableSetId(), 
							result.getPromptId(), JsonUtils.getStringFromJsonArray(responseArray, j)
						);
						
						if(null == value) { // ok, get the label
//							Object label = config.getLabelForChoiceKey(result.getSurveyId(), result.getRepeatableSetId(), 
//								result.getPromptId(), JsonUtils.getStringFromJsonArray(responseArray, j)
//							);
//							
//							result.setDisplayValue(label);
							break;
							
						} else {
							
							valueArray.put(value);
						}
					}
					
					if(null == result.getDisplayValue()) {
						result.setDisplayValue(valueArray);
					}
					
				} else {
					
					if(PromptTypeUtils.isNumberPromptType(result.getPromptType())) { // check for a number to avoid numbers being
                                                                                     // quoted in the JSON output
						result.setDisplayValue(toNumber(result.getResponse())); 

					} else {

						result.setDisplayValue(result.getResponse());
					}
				}
			}
		}
		
		if(_logger.isDebugEnabled()) {
			_logger.debug(req.getResultList());
		}
	}
	
	private Number toNumber(Object object) {
		try {
			
			return Integer.parseInt(String.valueOf(object));
			
		} catch(NumberFormatException nfe) {  
			
			// ok, maybe it's a float
			
			try {
				
				return Float.parseFloat(String.valueOf(object));
				
			} catch (NumberFormatException nfe2) {
				
				_logger.warn("found value that is unparseable as a number: " + object);
			
			}
		}
		
		return null;
	}
}
