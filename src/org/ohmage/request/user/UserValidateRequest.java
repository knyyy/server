package org.ohmage.request.user;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.ohmage.exception.InvalidRequestException;
import org.ohmage.request.UserRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by kennetvuong on 15/04/15.
 */
public class UserValidateRequest extends UserRequest {
    private static final Logger LOGGER = Logger.getLogger(UserValidateRequest.class);
    /**
     * The key used to place the stats information into the JSON result.
     */
    public static final String JSON_KEY_RESULT = "validate";


    public UserValidateRequest(HttpServletRequest httpRequest) throws IOException, InvalidRequestException {
        super(httpRequest, false, TokenLocation.EITHER,  null);
    }

    @Override
    public void service() {
        authenticate(AllowNewAccount.NEW_ACCOUNT_DISALLOWED);
    }

    @Override
    public void respond(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super.respond(httpRequest,httpResponse,JSON_KEY_RESULT, (getUser() == null) ? false : true);
    }
}
