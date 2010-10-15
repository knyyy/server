package edu.ucla.cens.awserver.service;

import java.util.ArrayList;
import java.util.List;

import edu.ucla.cens.awserver.cache.CacheService;
import edu.ucla.cens.awserver.dao.Dao;
import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.RetrieveConfigAwRequest;

/**
 * @author selsky
 */
public class UserListService extends AbstractDaoService {
	// private static Logger _logger = Logger.getLogger(UserListService.class);
	private CacheService _userRoleCacheService;
	
	public UserListService(Dao dao, CacheService cacheService) {
		super(dao);
		if(null == cacheService) {
			throw new IllegalArgumentException("a CacheSercice is required");
		}
		_userRoleCacheService = cacheService;
	}
	
	/**
	 * For researchers and admins, pushes the users that belong to the current campaign into the AwRequest. Also sets the user role
	 * string for the current User. The AwRequest must be a RetrieveConfigAwRequest. 
	 */
	@Override
	public void execute(AwRequest awRequest) {
		RetrieveConfigAwRequest retrieveConfigAwRequest = (RetrieveConfigAwRequest) awRequest;
		boolean isAdminOrResearcher = false;
		
		List<Integer> list = awRequest.getUser().getCampaignRoles().get(awRequest.getCampaignName());
		
		for(Integer i : list) {
			
			String role = (String) _userRoleCacheService.lookup(i);
			
			if("researcher".equals(role) || "admin".equals(role)) {
				isAdminOrResearcher = true;
				retrieveConfigAwRequest.setOutputUserRole(role);
				break;
			}
		}
		
		List<String> userList = new ArrayList<String>();
		
		if(! isAdminOrResearcher) {
			retrieveConfigAwRequest.setOutputUserRole("participant");
		}
		
		if(isAdminOrResearcher) {
			
			getDao().execute(retrieveConfigAwRequest);
			
			List<?> results = retrieveConfigAwRequest.getResultList();
			int size = results.size();
			
			if(0 == size) { // logical error! There must be at least one user found  (the current user from the AwRequest)
				throw new ServiceException("no users found for campaign");
			}
			
			for(int i = 0; i < size; i++) {
				userList.add((String) results.get(i));
			}
			
			
		} else {
			
			userList.add(retrieveConfigAwRequest.getUser().getUserName());
			
		}
		
		retrieveConfigAwRequest.setOutputUserList(userList);
	}
}
