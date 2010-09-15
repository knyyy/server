package edu.ucla.cens.awserver.jee.servlet.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class SurveyUploadValidator extends AbstractHttpServletRequestValidator {
	private static Logger _logger = Logger.getLogger(SurveyUploadValidator.class);
	private List<String> _parameterList;
	
	/**
	 * 
	 */
	public SurveyUploadValidator() {
		_parameterList = new ArrayList<String>(Arrays.asList(new String[]{"u","ci","d","p","c","cv"}));
	}
	
	
	@Override
	public boolean validate(HttpServletRequest request) {
		Map<?,?> parameterMap = request.getParameterMap(); // String, String[]
		
		// Check for missing or extra parameters
		
		if(parameterMap.size() != _parameterList.size()) {
			_logger.warn("an incorrect number of parameters was found on phone mobility upload: " + parameterMap.size());
			return false;
		}
		
		// Check for duplicate parameters
		
		Iterator<?> iterator = parameterMap.keySet().iterator();
		
		while(iterator.hasNext()) {
			String key = (String) iterator.next();
			String[] valuesForKey = (String[]) parameterMap.get(key);
			
			if(valuesForKey.length != 1) {
				_logger.warn("an incorrect number of values (" + valuesForKey.length + ") was found for parameter " + key);
				return false;
			}
		}
		
		// Check for parameters with unknown names
		
		iterator = parameterMap.keySet().iterator(); // there is no way to reset the iterator so just obtain a new one
		
		while(iterator.hasNext()) {
			String name = (String) iterator.next();
			if(! _parameterList.contains(name)) {
			
				_logger.warn("an incorrect parameter name was found: " + name);
				return false;
			}
		}
		
		String u = (String) request.getParameter("u");
		String p = (String) request.getParameter("p");
		String ci = (String) request.getParameter("ci");
		String c = (String) request.getParameter("c");
		String cv = (String) request.getParameter("cv");
		
		// Check for abnormal lengths (buffer overflow attack)
		// In general, the max lengths below are based on receiving %-encoded characters for every char at the max length
		// allowed by our db 
		
		if(greaterThanLength("user", "u", u, 75)
		   || greaterThanLength("client", "ci", ci, 600)
		   || greaterThanLength("campaign name", "c", c, 750)
		   || greaterThanLength("campaign version", "cv", cv, 750)
		   || greaterThanLength("password", "p", p, 180) // handle up to 60 %-encoded characters
		) {
			return false;
		}
		
		// The JSON data is not checked because its length is so variable and potentially huge (some messages are 700000+ characters
		// when URL-encoded). It will be heavily validated once inside the main application validation layer.
		
		// The default setting for Tomcat is to disallow requests that are greater than 2MB
		
		return true;
	}

}
