/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.oauth2provider.cache;

import org.apache.log4j.Logger;
import org.ohmage.domain.User;
import org.ohmage.exception.DomainException;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides an in-memory relation between access tokens and the time at which they expire. The relation is
 * populated from a database table when the server boots.
 *
 * Copied from {@link org.ohmage.cache.UserBin}. Notable differences: the expiration time is stored
 * rather than the time at which the entry was inserted in order to allow for different expiration times for
 * different tokens.
 * 
 * @author Joshua Selsky
 * @author Faisal Alquaddoomi
 */
public final class HandleBin extends TimerTask implements DisposableBean {
	private static final Logger LOGGER = Logger.getLogger(HandleBin.class);

	// how frequently the hashmap is pruned for expired entries
	private static final int EXECUTION_PERIOD = 60000; // 60 seconds

	/**
	 * A class for associating handle owners to the time their token expires. Based on the UserTime
     * class from UserBin.
	 *
	 * @author John Jenkins
     * @author Faisal Alquaddoomi
	 */
	private static final class HandleTime {
		private final User user;
		private long time;

		/**
		 * Convenience constructor.
		 *
		 * @param user The user that is being stored in the cache.
		 *
		 * @param time The time at which this token is set to expire.
		 */
		private HandleTime(User user, long time) {
			this.user = user;
			this.time = time;
		}
	}

	// A map of tokens to USERS and the time that their token expires.
	private static final Map<String, HandleTime> USERS = new ConcurrentHashMap<String, HandleTime>();
	// An EXECUTIONER thread to purge those whose tokens have expired.
	private static final Timer EXECUTIONER = new Timer("HandleBin - handle expiration process.", true);

	// Whether or not the constructor has run which will bootstrap this
	// Singleton class.
	private static boolean initialized = false;

	private HandleBin() {
		LOGGER.info("HandleBin executioner will run every " + EXECUTION_PERIOD + " milliseconds");
		EXECUTIONER.schedule(this, EXECUTION_PERIOD * 2, EXECUTION_PERIOD);
		initialized = true;
	}
	
	@Override
	public void destroy() {
		EXECUTIONER.cancel();
	}

    // TODO: write method to load up existing handles from the database
	
	/**
	 * Adds a user to the bin and returns an Id (token) representing that user. If the user is already resident in the bin, their
	 * old token is removed and a new one is generated and returned.
     *
     * @param duration How long this token will last in milliseconds
	 */
	public static synchronized String addUser(User user, long duration)
			throws DomainException {
		
		if (!initialized) {
			new HandleBin();
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("adding user to bin");
		}
		
		String uuid = UUID.randomUUID().toString();
		HandleTime ut = new HandleTime(user, System.currentTimeMillis() + duration);
		user.setToken(uuid);
		if(USERS.put(uuid, ut) != null) {
			throw new DomainException("UUID collision: " + uuid);
		}

        // TODO: we should write these through to the database (later)
		
		return uuid;
	}
	
	/**
	 * Removes a user from the user bin.
	 * 
	 * @param authToken The authentication token to remove from the user bin.
	 */
	public static synchronized void expireUser(String authToken) {
		if(! initialized) {
			new HandleBin();
		}
		
		if(authToken == null) {
			throw new IllegalArgumentException("The token cannot be null.");
		}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Removing user from bin.");
		}
		
		USERS.remove(authToken);
	}
	
	/**
	 * Removes all of the tokens for some handle owner.
	 * 
	 * @param username The handle owner's username.
	 */
	public static synchronized void removeUser(String username) {
		if(! initialized) {
			new HandleBin();
		}
		
		if(username == null) {
			throw new IllegalArgumentException("The handle owner's name cannot be null.");
		}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Removing the handle from the bin.");
		}
		
		Set<String> userTokens = new HashSet<String>();
		for(String token : USERS.keySet()) {
			HandleTime userTime = USERS.get(token);
			if(userTime.user.getUsername().equals(username)) {
				userTokens.add(token);
			}
		}
		
		for(String token : userTokens) {
			USERS.remove(token);
		}
	}
	
	/**
	 * Returns the User bound to the provided Id or null if Id does not exist in the bin. 
	 */
	public static synchronized User getUser(String id) {
        HandleTime ut = USERS.get(id);
		if(null != ut) { 
			User u = ut.user;
			if(null != u) {
				ut.time = System.currentTimeMillis(); // refresh the time 
				try {
					return new User(u);
				} catch (DomainException e) {
					LOGGER.error("Error duplicating the handle.", e);
					return null;
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the number of milliseconds until a token expires.
	 * 
	 * @param id The token.
	 * 
	 * @return The number of milliseconds until 'id' expires.
	 */
	public static synchronized long getTokenRemainingLifetimeInMillis(String id) {
		HandleTime ut = USERS.get(id);
		if(ut == null) {
			return 0;
		}
		else {
			return Math.max(ut.time - System.currentTimeMillis(), 0);
		}
	}
	
	/**
	 * Background thread for purging expired Users.
	 */
	@Override
	public void run() {
		expire();
	}
	
	/**
	 * Checks every bin location and removes Users whose tokens have expired.
	 */
	private static synchronized void expire() {
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Beginning handle expiration process");
		}
		
		Set<String> keySet = USERS.keySet();
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Number of handles before expiration: " + keySet.size());
		}
		
		long currentTime = System.currentTimeMillis();
		
		for(String key : keySet) {
			HandleTime ut = USERS.get(key);
			if (ut.time - currentTime <= 0) {
			    	
				if(LOGGER.isDebugEnabled()) {
					LOGGER.debug("Removing handles with Id " + key);
				}
				
				USERS.remove(key);
			}
		}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Number of handles after expiration: " + USERS.size());
		}
	}
}
