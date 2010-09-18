package edu.ucla.cens.awserver.domain;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable bean-style wrapper for configuration properties.
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

	@Override
	public String toString() {
		return "Configuration [_campaignName=" + _campaignName
				+ ", _campaignVersion=" + _campaignVersion + ", _surveyMap="
				+ _surveyMap + "]";
	}
}
