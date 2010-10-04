package edu.ucla.cens.awserver.jee.servlet.glue;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.NDC;

import edu.ucla.cens.awserver.domain.UserImpl;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.UploadAwRequest;

/**
 * Transformer for creating an AwRequest for the upload feature.
 * 
 * @author selsky
 */
public class MobilityUploadAwRequestCreator implements AwRequestCreator {
//	private static Logger _logger = Logger.getLogger(SensorUploadAwRequestCreator.class);
	
	/**
	 * Default no-arg constructor. Simply creates an instance of this class.
	 */
	public MobilityUploadAwRequestCreator() {
		
	}
	
	/**
	 *  Pulls the u (userName), c (campaign), ci (client), and d (json data) parameters out of the HttpServletRequest and places
	 *  them in a new AwRequest.
	 */
	public AwRequest createFrom(HttpServletRequest request) {
		String sessionId = request.getSession(false).getId(); // for upload logging to connect app logs to upload logs
		
		String userName = request.getParameter("u");
		String campaignId = request.getParameter("c");
		String password = request.getParameter("p");
		String ci = request.getParameter("ci");
		String jsonData = null; 
		try {
			
			String jd = request.getParameter("d");
			
			if(null != jd) {
				jsonData = URLDecoder.decode(jd, "UTF-8");
			}
			
		} catch(UnsupportedEncodingException uee) { // if UTF-8 is not recognized we have big problems
			
			throw new IllegalStateException(uee);
		}
		
		UserImpl user = new UserImpl();
		user.setUserName(userName);
		user.setPassword(password);
		user.setCurrentCampaignId(campaignId);
		
		AwRequest awRequest = new UploadAwRequest();

		awRequest.setStartTime(System.currentTimeMillis());
		awRequest.setSessionId(sessionId);
		awRequest.setUser(user);
		awRequest.setClient(ci);
		awRequest.setJsonDataAsString(jsonData);
				
		String requestUrl = request.getRequestURL().toString();
		if(null != request.getQueryString()) {
			requestUrl += "?" + request.getQueryString(); 
		}
		
		awRequest.setRequestUrl(requestUrl); // placed in the request for use in logging messages
		
		NDC.push("ci=" + ci); // push the client string into the Log4J NDC for the currently executing thread - this means that it 
                              // will be in every log message for the thread
		
		return awRequest;
	}
}

