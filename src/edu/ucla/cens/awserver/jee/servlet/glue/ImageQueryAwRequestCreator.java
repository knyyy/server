package edu.ucla.cens.awserver.jee.servlet.glue;

import javax.servlet.http.HttpServletRequest;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.MediaQueryAwRequest;

/**
 * Builds an AwRequest for the image query API feature.
 * 
 * @author selsky
 */
public class ImageQueryAwRequestCreator implements AwRequestCreator {
	
	public ImageQueryAwRequestCreator() {
		
	}
	
	public AwRequest createFrom(HttpServletRequest request) {
		
		String userNameRequestParam = request.getParameter("u");
		String client = request.getParameter("ci");
		String campaignName = request.getParameter("c");
		String campaignVersion = request.getParameter("cv");
		String authToken = request.getParameter("t");
		String imageId = request.getParameter("i");  
		
		MediaQueryAwRequest awRequest = new MediaQueryAwRequest();

		awRequest.setUserNameRequestParam(userNameRequestParam);
		awRequest.setUserToken(authToken);
		awRequest.setClient(client);
		awRequest.setCampaignName(campaignName);
		awRequest.setMediaId(imageId);
		awRequest.setCampaignVersion(campaignVersion);
		
		return awRequest;
	}
}
