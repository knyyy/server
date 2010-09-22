package edu.ucla.cens.awserver.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable bean-style wrapper for accessing and validating configuration properties.
 * 
 * @author selsky
 */
public class Configuration {
	private String _campaignName;
	private String _campaignVersion;
	private Map<String, Survey> _surveyMap;
	
	public Configuration(String campaignName, String campaignVersion, Map<String, Survey> surveyMap) {
		if(null == campaignName) {
			throw new IllegalArgumentException("a campaignName is required");
		}
		if(null == campaignVersion) {
			throw new IllegalArgumentException("a campaignVersion is required");
		}
		if(null == surveyMap) {
			throw new IllegalArgumentException("a map of surveys is required");
		}
		
		_campaignName = campaignName;
		_campaignVersion = campaignVersion;
		_surveyMap = surveyMap; // TODO deep copy?
	}
	
	public String getCampaignName() {
		return _campaignName;
	}

	public String getCampaignVersion() {
		return _campaignVersion;
	}

	public Map<String, Survey> getSurveys() {
		return Collections.unmodifiableMap(_surveyMap);
	}
	
	public boolean surveyIdExists(String surveyId) {
		return _surveyMap.containsKey(surveyId);
	}
	
	public boolean repeatableSetExists(String surveyId, String repeatableSetId) {
        if(! _surveyMap.get(surveyId).getSurveyItemMap().containsKey(repeatableSetId)) {
        	return false;
        } 
		SurveyItem si = _surveyMap.get(surveyId).getSurveyItemMap().get(repeatableSetId);
        return si instanceof RepeatableSet;
	}

	public boolean promptExists(String surveyId, String promptId) {
        return _surveyMap.get(surveyId).getSurveyItemMap().containsKey(promptId);
	}
	
	public boolean promptExists(String surveyId, String repeatableSetId, String promptId) {
        return ((RepeatableSet)_surveyMap.get(surveyId).getSurveyItemMap().get(repeatableSetId)).getPromptMap().containsKey(promptId);
	}
	
	public boolean isPromptSkippable(String surveyId, String promptId) {
        return ((Prompt) _surveyMap.get(surveyId).getSurveyItemMap().get(promptId)).isSkippable();
	}
	
	public boolean isPromptSkippable(String surveyId, String repeatableSetId, String promptId) {
        return ((RepeatableSet)_surveyMap.get(surveyId).getSurveyItemMap().get(repeatableSetId)).getPromptMap().get(promptId).isSkippable();
	}

	public String getPromptType(String surveyId, String promptId) {
		return ((Prompt)_surveyMap.get(surveyId).getSurveyItemMap().get(promptId)).getType();
	}

	public String getPromptType(String surveyId, String repeatableSetId, String promptId) {
		return ((RepeatableSet)_surveyMap.get(surveyId).getSurveyItemMap().get(repeatableSetId)).getPromptMap().get(promptId).getType();
	}

	public Prompt getPrompt(String surveyId, String promptId) {
		return ((Prompt)_surveyMap.get(surveyId).getSurveyItemMap().get(promptId)); 
	}

	public Prompt getPrompt(String surveyId, String repeatableSetId, String promptId) {
		return ((RepeatableSet)_surveyMap.get(surveyId).getSurveyItemMap().get(repeatableSetId)).getPromptMap().get(promptId); 
	}
	
	/**
	 * Returns the number of prompts in the repeatable set inside the survey represented by survey id. Assumes that surveyId and
	 * repeatableSetId are valid. 
	 */
	public int numberOfPromptsInRepeatableSet(String surveyId, String repeatableSetId) {
		SurveyItem si = _surveyMap.get(surveyId).getSurveyItemMap().get(repeatableSetId);
        return ((RepeatableSet) si).getPromptMap().size();
	}

	@Override
	public String toString() {
		return "Configuration [_campaignName=" + _campaignName
				+ ", _campaignVersion=" + _campaignVersion + ", _surveyMap="
				+ _surveyMap + "]";
	}
}
