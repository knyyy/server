package edu.ucla.cens.awserver.domain;

/**
 * Represents a row from the prompt_type table.
 * 
 * @author selsky
 */
public class PromptType {
	private int _id;
	private String _type;
	
	public int getId() {
		return _id;
	}
	
	public void setId(int id) {
		_id = id;
	}
	
	public String getType() {
		return _type;
	}
	
	public void setType(String type) {
		_type = type;
	}

	@Override
	public String toString() {
		return "PromptType [_id=" + _id + ", _type=" + _type + "]";
	}
}
