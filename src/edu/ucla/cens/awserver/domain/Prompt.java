package edu.ucla.cens.awserver.domain;

import java.util.Collections;
import java.util.List;

/**
 * A survey prompt configuration.
 * 
 * @author selsky
 */
public class Prompt extends AbstractSurveyItem {
	private String _displayType;
	private String _type;
	private List<PromptProperty> _properties;
	private boolean _skippable;

	public Prompt(String id, String displayType, String type, List<PromptProperty> props, boolean skippable) {
		super(id);
		_displayType = displayType;
		_type = type;
		_properties = props; // TODO really need a deep copy here
		_skippable = skippable;
	}

	public String getDisplayType() {
		return _displayType;
	}

	public String getType() {
		return _type;
	}

	public List<PromptProperty> getProperties() {
		return Collections.unmodifiableList(_properties);
	}

	public boolean isSkippable() {
		return _skippable;
	}

	@Override
	public String toString() {
		return "Prompt [_displayType=" + _displayType + ", _properties="
				+ _properties + ", _skippable=" + _skippable + ", _type="
				+ _type + ", getId()=" + getId() + "]";
	}
}
