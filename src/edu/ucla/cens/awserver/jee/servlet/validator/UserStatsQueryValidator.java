package edu.ucla.cens.awserver.jee.servlet.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * Validator for inbound data to the user stats API.
 * 
 * @author selsky
 */
public class UserStatsQueryValidator extends AbstractHttpServletRequestValidator {
	private static Logger _logger = Logger.getLogger(UserStatsQueryValidator.class);
	private List<String> _parameterList;
	
	/**
	 */
	public UserStatsQueryValidator() {
		_parameterList = new ArrayList<String>(Arrays.asList(new String[]{"u","c","ci","t"}));
	}
	
	/**
	 * 
	 */
	public boolean validate(HttpServletRequest httpServletRequest) {
		
		Map<?,?> parameterMap = httpServletRequest.getParameterMap(); // String, String[]
		
		// Check for missing or extra parameters
		if(parameterMap.size() != _parameterList.size()) {				
			_logger.warn("an incorrect number of parameters was found for an data point query: " + parameterMap.size());
			return false;
		}
		
		// ------- This could all be moved to a separate class
		
		// Check for duplicate parameters
		
		Iterator<?> iterator = parameterMap.keySet().iterator();
		
		while(iterator.hasNext()) {
			String key = (String) iterator.next();
			String[] valuesForKey = (String[]) parameterMap.get(key);
			
			if(valuesForKey.length != 1 && ! "i".equals(key)) {
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
		
		// -------- end move to a separate class
		
		String c = (String) httpServletRequest.getParameter("c");
		String ci = (String) httpServletRequest.getParameter("ci");
		String t = (String) httpServletRequest.getParameter("t");
		String u = (String) httpServletRequest.getParameter("u");
		
		// Check for abnormal lengths (buffer overflow attack)
		// The lengths are pretty arbitrary, but values exceeded them would be very strange
		
		if(greaterThanLength("campaignName", "c", c, 250)
		   || greaterThanLength("client", "ci",ci, 500)		   
		   || greaterThanLength("authToken", "t", t, 50)
		   || greaterThanLength("userName", "u", u, 75)) {
			
			_logger.warn("found an input parameter that exceeds its allowed length");
			return false;
		}
		
		return true;
	}
}
