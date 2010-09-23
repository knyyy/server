package edu.ucla.cens.awserver.domain;

public class PromptResponseDataPacket implements DataPacket {
	private String _promptId;   
	private String _repeatableSetId; 
	private String _value; 
	
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

	@Override
	public String toString() {
		return "PromptResponseDataPacket [_promptId=" + _promptId
				+ ", _repeatableSetId=" + _repeatableSetId + ", _value="
				+ _value + "]";
	}
}
