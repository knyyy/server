package edu.ucla.cens.awserver.domain;

import java.util.Collections;
import java.util.List;

public class RepeatableSet extends AbstractSurveyItem {
	private List<Prompt> _prompts;
	
	public RepeatableSet(String id, List<Prompt> prompts) {
		super(id);
		_prompts = prompts;
	}

	public List<Prompt> getPrompts() {
		return Collections.unmodifiableList(_prompts);
	}

	@Override
	public String toString() {
		return "RepeatableSet [_prompts=" + _prompts + ", getId()=" + getId()
				+ "]";
	}
}
