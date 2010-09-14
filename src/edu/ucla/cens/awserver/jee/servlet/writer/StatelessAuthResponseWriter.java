package edu.ucla.cens.awserver.jee.servlet.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.ucla.cens.awserver.request.AwRequest;

/**
 * @author selsky
 */
public class StatelessAuthResponseWriter extends AbstractResponseWriter {
	private static Logger _logger = Logger.getLogger(StatelessAuthResponseWriter.class);
	
	@Override
	public void write(HttpServletRequest request, HttpServletResponse response, AwRequest awRequest) {
		Writer writer = null;
		
		try {
			// Prepare for sending the response to the client
			writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(request, response)));
			String responseText = null;
			expireResponse(response);
			response.setContentType("application/json");
			
			// Build the appropriate response 
			if(! awRequest.isFailedRequest()) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("result", "success");
				
				Set<String> keys = awRequest.getUser().getCampaignRoles().keySet();
				Iterator<String> iterator = keys.iterator();
				
				JSONArray jsonArray = new JSONArray();
					
				while(iterator.hasNext()) {
					jsonArray.put(iterator.next());
				}
				
				jsonObject.put("campaigns", jsonArray);
				
				responseText = jsonObject.toString();
				
			} else {
				
				responseText = awRequest.getFailedRequestErrorMessage();
			}
			
			_logger.info("about to write output");
			writer.write(responseText);
		}
		
		catch(Exception e) { // catch Exception in order to avoid redundant catch block functionality (Java 7 will have 
			                 // comma-separated catch clauses) 
			
			_logger.error("an unrecoverable exception occurred while running an EMA query", e);
			try {
				
				writer.write("{\"code\":\"0103\",\"text\":\"" + e.getMessage() + "\"}");
				
			} catch (IOException ioe) {
				
				_logger.error("caught IOException when attempting to write to HTTP output stream: " + ioe.getMessage());
			}
			
		} finally {
			
			if(null != writer) {
				
				try {
					
					writer.flush();
					writer.close();
					writer = null;
					
				} catch (IOException ioe) {
					
					_logger.error("caught IOException when attempting to free resources: " + ioe.getMessage());
				}
			}
		}
	}
}
