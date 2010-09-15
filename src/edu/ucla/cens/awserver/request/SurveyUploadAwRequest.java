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
	private List<DataPacket> _dataPackets;  // JSON data converted into an internal representation
	
	/**
	 * Default no-arg constructor.	
	 */
	public SurveyUploadAwRequest() {
		_currentMessageIndex = -1;
		_currentPromptId = -1;
	}
	
	public List<Integer> getDuplicateIndexList() {
		return _duplicateIndexList;
	}
	
	public void setDuplicateIndexList(List<Integer> duplicateIndexList) {
		_duplicateIndexList = duplicateIndexList;
	}
	
	public Map<Integer, List<Integer>> getDuplicatePromptResponseMap() {
		return _duplicatePromptResponseMap;
	}
	
	public void setDuplicatePromptResponseMap(Map<Integer, List<Integer>> duplicatePromptResponseMap) {
		_duplicatePromptResponseMap = duplicatePromptResponseMap;
	}
	
	public long getStartTime() {
		return _startTime;
	}
	
	public void setStartTime(long startTime) {
		_startTime = startTime;
	}
	
	public String getSessionId() {
		return _sessionId;
	}
	
	public void setSessionId(String sessionId) {
		_sessionId = sessionId;
	}
	
	public String getClient() {
		return _client;
	}
	
	public void setClient(String client) {
		_client = client;
	}
	
	public String getJsonDataAsString() {
		return _jsonDataAsString;
	}
	
	public void setJsonDataAsString(String jsonDataAsString) {
		_jsonDataAsString = jsonDataAsString;
	}
	
	public List<DataPacket> getDataPackets() {
		return _dataPackets;
	}
	
	public void setDataPackets(List<DataPacket> dataPackets) {
		_dataPackets = dataPackets;
	}
	
	public int getCurrentMessageIndex() {
		return _currentMessageIndex;
	}
	
	public void setCurrentMessageIndex(int currentMessageIndex) {
		_currentMessageIndex = currentMessageIndex;
	}
	
	public int getCurrentPromptId() {
		return _currentPromptId;
	}
	
	public void setCurrentPromptId(int currentPromptId) {
		_currentPromptId = currentPromptId;
	}
	
	public JSONArray getJsonDataAsJsonArray() {
		return _jsonDataAsJsonArray;
	}
	
	public void setJsonDataAsJsonArray(JSONArray jsonDataAsJsonArray) {
		_jsonDataAsJsonArray = jsonDataAsJsonArray;
	}
	
	public List<?> getResultList() {
		return _resultList;
	}
	
	public void setResultList(List<?> resultList) {
		_resultList = resultList;
	}
	
	public String getCampaignVersion() {
		return _campaignVersion;
	}
	
	public void setCampaignVersion(String campaignVersion) {
		_campaignVersion = campaignVersion;
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

