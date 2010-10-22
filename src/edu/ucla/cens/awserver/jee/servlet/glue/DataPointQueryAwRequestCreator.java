package edu.ucla.cens.awserver.jee.servlet.glue;

import javax.servlet.http.HttpServletRequest;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.DataPointQueryAwRequest;

/**
 * Builds an AwRequest for the data point API feature.
 * 
 * @author selsky
 */
public class DataPointQueryAwRequestCreator implements AwRequestCreator {
	
	public DataPointQueryAwRequestCreator() {
		
	}
	
	/**
	 * 
	 */
	public AwRequest createFrom(HttpServletRequest request) {
		// HttpSession session = request.getSession();
		
		// Need to grab the user from the User Cache
		// User user = new UserImpl((User) session.getAttribute("user"));
		
		String startDate = request.getParameter("s");
		String endDate = request.getParameter("e");
		String userNameRequestParam = request.getParameter("u"); // researchers are allowed to pass an optional user name
		String client = request.getParameter("ci");
		String campaignName = request.getParameter("c");
		String authToken = request.getParameter("t");
		String dataPointId = request.getParameter("i");
		
		DataPointQueryAwRequest awRequest = new DataPointQueryAwRequest();
//		awRequest.setUser(user);
		awRequest.setStartDate(startDate);
		awRequest.setEndDate(endDate);
		awRequest.setUserNameRequestParam(userNameRequestParam);
		awRequest.setUserToken(authToken);
		awRequest.setClient(client);
		awRequest.setCampaignName(campaignName);
		awRequest.setDataPointId(dataPointId);
		
		return awRequest;
	}
}
