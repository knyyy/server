package edu.ucla.cens.awserver.cache;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.domain.User;
import edu.ucla.cens.awserver.domain.UserImpl;
import edu.ucla.cens.awserver.domain.UserTime;

/**
 * User storage. User objects are mapped to unique ids. Avoids dependencies on JEE session management. The lifetime param set on 
 * construction controls how long User objects stay active. 
 * 
 * TODO private locking?
 * 
 * @author selsky
 */
public class UserBin extends TimerTask {
	private static Logger _logger = Logger.getLogger(UserBin.class);
	private Map<String, UserTime> _users;
	private int _lifetime;
	private Timer _executioner;
		
	/**
	 * @param lifetime controls the number of milliseconds a User object will be permitted to be resident in the bin
	 * 
	 * TODO Enforce a max lifetime?
	 * TODO Enforce period relative to lifetime?
	 */
	public UserBin(int lifetime, int executionPeriod) {
		_lifetime = lifetime;
		_users = new ConcurrentHashMap<String, UserTime> ();
		_executioner = new Timer("user bin user expiration process", true);
		_executioner.schedule(this, executionPeriod * 2, executionPeriod);
	}
	
	public synchronized String addUser(User user) {
		if(_logger.isDebugEnabled()) {
			_logger.debug("adding user to bin");
		}
		String uuid = UUID.randomUUID().toString();
		UserTime ut = new UserTime(user, System.currentTimeMillis());
		_users.put(uuid, ut);
		return uuid;
	}
	
	public synchronized User getUser(String id) {
		User u = _users.get(id).getUser();
		if(null != u) {
			return new UserImpl(u); // lazy to assume that UserImpl is always what's needed, but it is the only User 
			                        // implementation for now.
		}
		return null;
	}
	
	private synchronized void expire() {
		if(_logger.isDebugEnabled()) {
			_logger.debug("beginning user expiration process");
		}
		
		Set<String> keySet = _users.keySet();
		if(_logger.isDebugEnabled()) {
			_logger.debug("number of users before expiration: " + keySet.size());
		}
		
		long currentTime = System.currentTimeMillis();
		
		for(String key : keySet) {
			UserTime ut = _users.get(key);
			if(currentTime - ut.getTime() > _lifetime) {
			    	
				if(_logger.isDebugEnabled()) {
					_logger.debug("removing user with id " + key);
				}
				
				_users.remove(key);
			}
		}
		
		if(_logger.isDebugEnabled()) {
			_logger.debug("number of users after expiration: " + _users.size());
		}
	}
	
	public void run() {
		expire();
	}
}
