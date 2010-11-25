package edu.ucla.cens.awserver.domain;

import java.util.List;


/**
 * Data packet implementation for the storage of a survey response: the metadata associated with a survey, the entire survey
 * itself as a String representation of JSON, and each prompt response.
 * 
 * @author selsky
 */
public class SurveyDataPacket extends MetadataDataPacket {
	private String _survey;
	private String _surveyId;
	private String _launchContext;
	private List<PromptResponseDataPacket> _responses;
	private int _surveyResponseKey = -1;
	
	public String getLaunchContext() {
		return _launchContext;
	}

	public void setLaunchContext(String launchContext) {
		_launchContext = launchContext;
	}

	public String getSurvey() {
		return _survey;
	}

	public void setSurvey(String survey) {
		_survey = survey;
	}

	public String getSurveyId() {
		return _surveyId;
	}

	public void setSurveyId(String surveyId) {
		_surveyId = surveyId;
	}
	
	public List<PromptResponseDataPacket> getResponses() {
		return _responses;
	}

	public void setResponses(List<PromptResponseDataPacket> responses) {
		_responses = responses;
	}
	
	public int getSurveyResponseKey() {
		return _surveyResponseKey;
	}

	public void setSurveyResponseKey(int surveyResponseKey) {
		_surveyResponseKey = surveyResponseKey;
	}

	@Override
	public String toString() {
		return "SurveyDataPacket [_launchContext=" + _launchContext
				+ ", _responses=" + _responses + ", _survey=" + _survey
				+ ", _surveyId=" + _surveyId + ", _surveyResponseKey="
				+ _surveyResponseKey + ", toString()=" + super.toString() + "]";
	}
}
