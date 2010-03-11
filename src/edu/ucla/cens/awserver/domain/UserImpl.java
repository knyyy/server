package edu.ucla.cens.awserver.domain;


/**
 * The default user implementation.
 * 
 * @author selsky
 */
public class UserImpl implements User {
	private int _id;
	private String  _userName;
    private int _campaignId;
	private boolean _loggedIn;
	private String _password;
	
	public UserImpl() {
		
	}
	
	/**
	 * Copy constructor.
	 */
	public UserImpl(User user) {
		if(null == user) {
			throw new IllegalArgumentException("a null user is not allowed");
		}
		_id = user.getId();
		_userName = user.getUserName();
		_campaignId = user.getCampaignId();
		_loggedIn = user.isLoggedIn();
	}
	
    public int getId() {
    	return _id;
    }
    
    public void setId(int id) {
    	_id = id;
    }
    
	public int getCampaignId() {
		return _campaignId;
	}
	
	public void setCampaignId(int id) {
		_campaignId = id;
	}
	
	public String getUserName() {
		return _userName;
	}

	public void setUserName(String userName) {
		_userName = userName;
	}
	
	public boolean isLoggedIn() {
		return _loggedIn;
	}
	
	public void setLoggedIn(boolean loggedIn) {
		_loggedIn = loggedIn;
	}
	
	public void setPassword(String password) {
		_password = password;
	}

	public String getPassword() {
		return _password;
	}
	
	@Override
	public String toString() { // _password is deliberately omitted here
		return "UserImpl [_campaignId=" + _campaignId + ", _id=" + _id
				+ ", _loggedIn=" + _loggedIn + ", _userName=" + _userName + "]";
	}
}
