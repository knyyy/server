package edu.ucla.cens.awserver.domain;

/**
 * @author selsky
 */
public class PromptType {

	private String _type;
	private String _restriction;
	private int _promptConfigId; // not the primary key for the prompt
	
	public int getPromptConfigId() {
		return _promptConfigId;
	}
	public void setPromptConfigId(int promptConfigId) {
		_promptConfigId = promptConfigId;
	}
	public String getType() {
		return _type;
	}
	public void setType(String type) {
		_type = type;
	}
	public String getRestriction() {
		return _restriction;
	}
	public void setRestriction(String restriction) {
		_restriction = restriction;
	}
	@Override
	public String toString() {
		return "PromptType [_promptConfigId=" + _promptConfigId
				+ ", _restriction=" + _restriction + ", _type=" + _type + "]";
	}
	
}
