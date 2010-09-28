package edu.ucla.cens.awserver.domain;

public class PromptResponseDataPacket implements DataPacket {
	private String _promptId;   
	private String _repeatableSetId; 
	private String _value;
	private String _type;
	
	public PromptResponseDataPacket() { }
	
	public String getPromptId() {
		return _promptId;
	}
	
	public void setPromptId(String promptId) {
		_promptId = promptId;
	}
	
	public void setRepeatableSetId(String repeatableSetId) {
		_repeatableSetId = repeatableSetId;
	}
	
	public String getRepeatableSetId() {
		return _repeatableSetId;
	}
	
	public String getValue() {
		return _value;
	}
	
	public void setValue(String value) {
		_value = value;
	}

	public String getType() {
		return _type;
	}

	public void setType(String type) {
		_type = type;
	}
		
}
