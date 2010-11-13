package edu.ucla.cens.awserver.domain;


/**
 * Data transfer object for returning results from multiple user stat queries.
 * 
 * @author selsky
 */
public class UserStatsQueryResult {
	private double _hoursSinceLastActivity;
	private String _maxFieldLabel;
	private String _userName; // a user name is kept here in addition to the user name found in the SurveyActivityQueryResult and 
	                          // the MobilityActivityQueryResult because there is a case boht of those query result objects
	                          // will be null (i.e., no records found for either)

	private SurveyActivityQueryResult _surveyActivityQueryResult;
	private MobilityActivityQueryResult _mobilityActivityQueryResult;
	private UserPercentage _surveyLocationUpdatesPercentage;
	private UserPercentage _mobilityLocationUpdatesPercentage;

	public String getUserName() {
		return _userName;
	}

	public void setUserName(String userName) {
		_userName = userName;
	}
	
	public String getMaxFieldLabel() {
		return _maxFieldLabel;
	}

	public void setMaxFieldLabel(String maxFieldLabel) {
		_maxFieldLabel = maxFieldLabel;
	}

	public double getHoursSinceLastActivity() {
		return _hoursSinceLastActivity;
	}
	
	public void setHoursSinceLastActivity(double hoursSinceLastActivity) {
		_hoursSinceLastActivity = hoursSinceLastActivity;
	}
	
	public SurveyActivityQueryResult getSurveyActivityQueryResult() {
		return _surveyActivityQueryResult;
	}
	
	public void setSurveyActivityQueryResult(SurveyActivityQueryResult promptActivityQueryResult) {
		_surveyActivityQueryResult = promptActivityQueryResult;
	}
	
	public MobilityActivityQueryResult getMobilityActivityQueryResult() {
		return _mobilityActivityQueryResult;
	}
	
	public void setMobilityActivityQueryResult(MobilityActivityQueryResult mobilityActivityQueryResult) {
		_mobilityActivityQueryResult = mobilityActivityQueryResult;
	}

	public UserPercentage getSurveyLocationUpdatesPercentage() {
		return _surveyLocationUpdatesPercentage;
	}

	public void setSurveyLocationUpdatesPercentage(UserPercentage surveyLocationUpdatesPercentage) {
		_surveyLocationUpdatesPercentage = surveyLocationUpdatesPercentage;
	}

	public UserPercentage getMobilityLocationUpdatesPercentage() {
		return _mobilityLocationUpdatesPercentage;
	}

	public void setMobilityLocationUpdatesPercentage(UserPercentage mobilityLocationUpdatesPercentage) {
		_mobilityLocationUpdatesPercentage = mobilityLocationUpdatesPercentage;
	}

	@Override
	public String toString() {
		return "UserStatsQueryResult [_hoursSinceLastActivity="
				+ _hoursSinceLastActivity + ", _maxFieldLabel="
				+ _maxFieldLabel + ", _mobilityActivityQueryResult="
				+ _mobilityActivityQueryResult
				+ ", _mobilityLocationUpdatesPercentage="
				+ _mobilityLocationUpdatesPercentage
				+ ", _surveyActivityQueryResult=" + _surveyActivityQueryResult
				+ ", _surveyLocationUpdatesPercentage="
				+ _surveyLocationUpdatesPercentage + ", _userName=" + _userName
				+ "]";
	}
}
