package edu.ucla.cens.awserver.validator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.util.ReflectionUtils;
import edu.ucla.cens.awserver.util.StringUtils;

/**
 * Validator that checks an AwRequest property to make sure that it exists and is a positive integer.
 * 
 * @author selsky
 */
public class PositiveIntegerExistsAnnotatingValidator extends AbstractAnnotatingValidator {
	private String _key;
	private Method _accessorMethod;
	
	/**
	 * @throws IllegalArgumentException if the provided key is null, empty, or all whitespace
	 * @see ReflectionUtils#getAccessorMethod(Class, String)
	 */
	public PositiveIntegerExistsAnnotatingValidator(AwRequestAnnotator annotator, String key) {
		super(annotator);
		if(StringUtils.isEmptyOrWhitespaceOnly(key)) {
			throw new IllegalArgumentException("a non-empty key is required");
		}
		
		_accessorMethod = ReflectionUtils.getAccessorMethod(AwRequest.class, key);
		_key = key;
	}

	/**
	 * @return true if the attribute with the accessor method defined by this instance's key is a positive integer
	 * @return false otherwise 
	 */
	public boolean validate(AwRequest awRequest) {
		try {
			
			String attrValue = (String) _accessorMethod.invoke(awRequest);
			
			if(! StringUtils.isEmptyOrWhitespaceOnly(attrValue)) {
			
				try {
					
					int i = Integer.parseInt(attrValue);
					
					if(i <= 0) {
						getAnnotator().annotate(awRequest, "invalid value found for " + _key + ". must be a positive int:  " + attrValue);
						return false;
					}
					
					return true;
					
					
				} catch (NumberFormatException nfe) {
					
					getAnnotator().annotate(awRequest, "invalid value found for " + _key + ". not an integer:  " + attrValue);
					return false;
					
				}
			}
			
			getAnnotator().annotate(awRequest, "invalid value found for " + _key + ". value:  " + attrValue);
			return false;
		}
		catch(InvocationTargetException ite) {
			
	    	throw new ValidatorException(ite);
		
		} catch(IllegalAccessException iae) {
			
			throw new ValidatorException(iae);
		}
	}
}
