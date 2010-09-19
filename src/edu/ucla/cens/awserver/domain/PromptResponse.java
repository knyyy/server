package edu.ucla.cens.awserver.domain;

public class PromptResponse {
	private String _promptId;
	private Object _value;
	
	public PromptResponse(String id, String value) {
		_promptId = id;
		_value = value;
	}

	public String getPromptId() {
		return _promptId;
	}

	public Object getValue() {
		return _value;
	}

	@Override
	public String toString() {
		return "PromptResponse [_promptId=" + _promptId + ", _value=" + _value
				+ "]";
	}
}
