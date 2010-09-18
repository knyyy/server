package edu.ucla.cens.awserver.domain;

import java.util.Collections;
import java.util.List;

/**
 * A minified immutable survey generated from configuration XML: only the properties necessary for data validation are present in  
 * this class (so, no messages, descriptions, etc). 
 * 
 * @author selsky
 */
public class Survey {
	private String _surveyId;
	private List<SurveyItem> _surveyItems; // prompts and repeatableSets
	
	public Survey(String surveyId, List<SurveyItem> surveyItems) {
		_surveyId = surveyId;
		_surveyItems = surveyItems; // TODO really need a deep copy here
	}

	public String getSurveyId() {
		return _surveyId;
	}

	public List<SurveyItem> getSurveyItems() {
		return Collections.unmodifiableList(_surveyItems);
	}

	@Override
	public String toString() {
		return "Survey [_surveyId=" + _surveyId + ", _surveyItems="
				+ _surveyItems + "]";
	}
}
