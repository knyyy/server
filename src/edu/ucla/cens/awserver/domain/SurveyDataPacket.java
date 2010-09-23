package edu.ucla.cens.awserver.domain;


/**
 * Data packet implementation for the storage of a survey response: the metadata associated with a survey and the entire survey
 * itself as a String representation of JSON.
 * 
 * @author selsky
 */
public class SurveyDataPacket extends MetadataDataPacket {
	private String _survey;
	private String _surveyId;
	
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

	@Override
	public String toString() {
		return "SurveyDataPacket [_survey=" + _survey + ", _surveyId="
				+ _surveyId + ", getAccuracy()=" + getAccuracy()
				+ ", getDate()=" + getDate() + ", getEpochTime()="
				+ getEpochTime() + ", getLatitude()=" + getLatitude()
				+ ", getLongitude()=" + getLongitude() + ", getProvider()="
				+ getProvider() + ", getTimezone()=" + getTimezone() + "]";
	}
}
