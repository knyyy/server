package edu.ucla.cens.awserver.validator.prompt;

import edu.ucla.cens.awserver.domain.Prompt;
import edu.ucla.cens.awserver.domain.PromptResponse;

/**
 * @author selsky
 */
public interface PromptValidator {
	/**
	 * Validates a PromptResponse against a configured Prompt. 
	 */
	boolean validate(Prompt prompt, PromptResponse promptResponse);
}
