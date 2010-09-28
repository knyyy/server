package edu.ucla.cens.awserver.request;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import edu.ucla.cens.awserver.domain.DataPacket;

/**
 * Represents state for survey uploads.
 * 
 * @author selsky
 */
public class SurveyUploadAwRequest extends ResultListAwRequest {
	// Input state //
	private String _client;
	private String _jsonDataAsString;
	private String _sessionId;
	private String _campaignVersion;
	
	// Processing state // 
	private long _startTime;              // processing start time for logging
	private List<?> _resultList; 	      
		
	private List<Integer> _duplicateIndexList; // store the indexes of duplicate mobility or prompt responses for logging
	private Map<Integer, List<Integer>> _duplicatePromptResponseMap; // prompts are uploaded in groups so a map stores
	                                                                 // a list of duplicate prompt responses at key by their  
	                                                                 // message index
	
	private int _currentMessageIndex; // used for logging errors based on the current invalid message
	private int _currentPromptId;     // used for logging errors for invalid prompts
	
	private JSONArray _jsonDataAsJsonArray; // The input data array converted to an internal JSON representation
	private List<DataPacket> _dataPackets;  // Surveys for insertion into database
	
	/**
	 * Default no-arg constructor.	
	 */
	public SurveyUploadAwRequest() {
		_currentMessageIndex = -1;
		_currentPromptId = -1;
	}
	
	public String getCampaignVersion() {
		return _campaignVersion;
	}
	
	public String getClient() {
		return _client;
	}
	
	public int getCurrentMessageIndex() {
		return _currentMessageIndex;
	}
	
	public int getCurrentPromptId() {
		return _currentPromptId;
	}
	
	public List<DataPacket> getDataPackets() {
		return _dataPackets;
	}
	
	public List<Integer> getDuplicateIndexList() {
		return _duplicateIndexList;
	}
	
	public Map<Integer, List<Integer>> getDuplicatePromptResponseMap() {
		return _duplicatePromptResponseMap;
	}
	
	public JSONArray getJsonDataAsJsonArray() {
		return _jsonDataAsJsonArray;
	}
	
	public String getJsonDataAsString() {
		return _jsonDataAsString;
	}
	
	public List<?> getResultList() {
		return _resultList;
	}
	
	public String getSessionId() {
		return _sessionId;
	}
	
	public long getStartTime() {
		return _startTime;
	}
	
	public void setCampaignVersion(String campaignVersion) {
		_campaignVersion = campaignVersion;
	}
	
	public void setClient(String client) {
		_client = client;
	}
	
	public void setCurrentMessageIndex(int currentMessageIndex) {
		_currentMessageIndex = currentMessageIndex;
	}
	
	public void setCurrentPromptId(int currentPromptId) {
		_currentPromptId = currentPromptId;
	}
	
	public void setDataPackets(List<DataPacket> dataPackets) {
		_dataPackets = dataPackets;
	}
	
	public void setDuplicateIndexList(List<Integer> duplicateIndexList) {
		_duplicateIndexList = duplicateIndexList;
	}
	
	public void setDuplicatePromptResponseMap(Map<Integer, List<Integer>> duplicatePromptResponseMap) {
		_duplicatePromptResponseMap = duplicatePromptResponseMap;
	}
	
	public void setJsonDataAsJsonArray(JSONArray jsonDataAsJsonArray) {
		_jsonDataAsJsonArray = jsonDataAsJsonArray;
	}
	
	public void setJsonDataAsString(String jsonDataAsString) {
		_jsonDataAsString = jsonDataAsString;
	}
	
	public void setResultList(List<?> resultList) {
		_resultList = resultList;
	}
	
	public void setSessionId(String sessionId) {
		_sessionId = sessionId;
	}
	
	public void setStartTime(long startTime) {
		_startTime = startTime;
	}
	

	@Override
	public String toString() {
		return "SurveyUploadAwRequest [_campaignVersion=" + _campaignVersion
				+ ", _client=" + _client + ", _currentMessageIndex="
				+ _currentMessageIndex + ", _currentPromptId="
				+ _currentPromptId + ", _dataPackets=" + _dataPackets
				+ ", _duplicateIndexList=" + _duplicateIndexList
				+ ", _duplicatePromptResponseMap="
				+ _duplicatePromptResponseMap + ", _jsonDataAsJsonArray="
				+ _jsonDataAsJsonArray + ", _jsonDataAsString="
				+ _jsonDataAsString + ", _resultList=" + _resultList
				+ ", _sessionId=" + _sessionId + ", _startTime=" + _startTime
				+ "]";
	}
}

