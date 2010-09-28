package edu.ucla.cens.awserver.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.ucla.cens.awserver.cache.CacheService;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.service.ServiceException;
import edu.ucla.cens.awserver.util.JsonUtils;

/**
 * @author selsky
 */
public class SurveyDataPacketBuilder extends AbstractDataPacketBuilder {
	private static Logger _logger = Logger.getLogger(SurveyDataPacketBuilder.class);
	private CacheService _configurationCacheService;
	
	public SurveyDataPacketBuilder(CacheService configurationCacheService) {
		if(null == configurationCacheService) {
			throw new IllegalArgumentException("a configuration cache service is required");
		}
		_configurationCacheService = configurationCacheService;
	}
	
	/**
	 * Creates a SurveyDataPacket from a survey upload. Assumes that the upload message is valid.
	 */
	public DataPacket createDataPacketFrom(JSONObject source, AwRequest awRequest) {
		SurveyDataPacket surveyDataPacket = new SurveyDataPacket();
		List<PromptResponseDataPacket> promptResponseDataPackets  = new ArrayList<PromptResponseDataPacket>();
		
		createCommonFields(source, surveyDataPacket);
		String surveyId = JsonUtils.getStringFromJsonObject(source, "survey_id");
		surveyDataPacket.setSurveyId(surveyId);
		
		JSONArray responses = JsonUtils.getJsonArrayFromJsonObject(source, "responses");
		surveyDataPacket.setSurvey(responses.toString());

		int arrayLength = responses.length();	
		
		for(int i = 0; i < arrayLength; i++) {
			JSONObject responseObject = JsonUtils.getJsonObjectFromJsonArray(responses, i);
			
			// Check to see if its a repeatable set
			String repeatableSetId = JsonUtils.getStringFromJsonObject(responseObject, "repeatable_set_id");
			
			if(null != repeatableSetId) {
				
				// ok, grab the inner responses - repeatable sets are anonymous objects in an array of arrays
				// get the outer array
				JSONArray outerArray = JsonUtils.getJsonArrayFromJsonObject(responseObject, "responses");
				_logger.info("outerArray.length()=" + outerArray.length());
				
				// now each element in the array is also an array
				for(int j = 0; j < outerArray.length(); j++) {
					JSONArray repeatableSetResponses =  JsonUtils.getJsonArrayFromJsonArray(outerArray, j);
					int numberOfRepeatableSetResponses = repeatableSetResponses.length(); _logger.info("numberOfRepeatableSetResponses=" + numberOfRepeatableSetResponses);
					for(int k = 0; k < numberOfRepeatableSetResponses; k++) { 
						PromptResponseDataPacket promptResponseDataPacket = new PromptResponseDataPacket();
						JSONObject rsPromptResponse = JsonUtils.getJsonObjectFromJsonArray(repeatableSetResponses, k);
						String promptId = JsonUtils.getStringFromJsonObject(rsPromptResponse, "prompt_id");
						promptResponseDataPacket.setPromptId(promptId);
						promptResponseDataPacket.setRepeatableSetId(repeatableSetId);
						Configuration configuration = (Configuration) _configurationCacheService.lookup(
							new CampaignNameVersion(awRequest.getUser().getCurrentCampaignName(), awRequest.getCampaignVersion())
						);
						promptResponseDataPacket.setType(configuration.getPromptType(surveyId, repeatableSetId, promptId));
						JSONObject customChoicesObject = JsonUtils.getJsonObjectFromJsonObject(rsPromptResponse, "custom_choices");
						if(null != customChoicesObject) {
							promptResponseDataPacket.setValue(customChoicesJsonString(rsPromptResponse));
						} else {
							// TODO will this autoconvert JSON objects to strings?
							promptResponseDataPacket.setValue(JsonUtils.getStringFromJsonObject(rsPromptResponse, "value"));
						}
						
						promptResponseDataPackets.add(promptResponseDataPacket);
					}
				}
				
			} else {
				
				PromptResponseDataPacket promptResponseDataPacket = new PromptResponseDataPacket();
				String promptId = JsonUtils.getStringFromJsonObject(responseObject, "prompt_id");
				promptResponseDataPacket.setPromptId(promptId);
				
				JSONObject customChoicesObject = JsonUtils.getJsonObjectFromJsonObject(responseObject, "custom_choices");
				if(null != customChoicesObject) {
					promptResponseDataPacket.setValue(customChoicesJsonString(responseObject));
				} else {
					// TODO will this autoconvert JSON array to strings?
					promptResponseDataPacket.setValue(JsonUtils.getStringFromJsonObject(responseObject, "value"));
				}
				
				promptResponseDataPacket.setRepeatableSetId(null);
				Configuration configuration = (Configuration) _configurationCacheService.lookup(
					new CampaignNameVersion(awRequest.getUser().getCurrentCampaignName(), awRequest.getCampaignVersion())
				);
				promptResponseDataPacket.setType(configuration.getPromptType(surveyId, promptId));
				promptResponseDataPackets.add(promptResponseDataPacket);
			}
		}

		surveyDataPacket.setResponses(promptResponseDataPackets);
		
		if(_logger.isDebugEnabled()) {
			_logger.debug(surveyDataPacket);
		}
		
		return surveyDataPacket;
	}
	
	public String customChoicesJsonString(JSONObject responseObject) {
		JSONObject copyObject = new JSONObject();
		try {
			copyObject.put("custom_choices", JsonUtils.getJsonObjectFromJsonObject(responseObject, "custom_choices"));
			copyObject.put("value", JsonUtils.getStringFromJsonObject(responseObject, "value"));
			
		} catch (JSONException jsone) {
			_logger.error("caught JSONException when attempting to build custom prompt response for db insertion", jsone);
			throw new ServiceException(jsone);
		}
		return copyObject.toString();
	}
}
