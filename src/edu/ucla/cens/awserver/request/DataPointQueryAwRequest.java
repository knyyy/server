package edu.ucla.cens.awserver.request;


/**
 * State for data point API queries.
 * 
 * @author selsky
 */
public class DataPointQueryAwRequest extends ResultListAwRequest {
	private String _startDate;
	private String _endDate;
	private String _userNameRequestParam;
	private String _client;
	private String _campaignName;
	private String _dataPointId;
	// private String _authToken; see userToken in parent class
	
	public String getStartDate() {
		return _startDate;
	}
	
	public void setStartDate(String startDate) {
		_startDate = startDate;
	}
	
	public String getEndDate() {
		return _endDate;
	}
	
	public void setEndDate(String endDate) {
		_endDate = endDate;
	}
	
	public String getUserNameRequestParam() {
		return _userNameRequestParam;
	}

	public void setUserNameRequestParam(String userNameRequestParam) {
		_userNameRequestParam = userNameRequestParam;
	}

	public String getClient() {
		return _client;
	}

	public void setClient(String client) {
		_client = client;
	}

	public String getCampaignName() {
		return _campaignName;
	}

	public void setCampaignName(String campaignName) {
		_campaignName = campaignName;
	}

	public String getDataPointId() {
		return _dataPointId;
	}

	public void setDataPointId(String dataPointId) {
		_dataPointId = dataPointId;
	}

//	public String getAuthToken() {
//		return _authToken;
//	}
//
//	public void setAuthToken(String authToken) {
//		_authToken = authToken;
//	}
}
