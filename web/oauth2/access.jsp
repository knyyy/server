<%@ page import="org.apache.amber.oauth2.common.message.OAuthResponse" %>
<%@ page import="org.apache.amber.oauth2.as.request.OAuthAuthzRequest" %>
<%@ page import="org.apache.amber.oauth2.as.response.OAuthASResponse" %>
<%@ page import="org.apache.amber.oauth2.as.issuer.OAuthIssuerImpl" %>
<%@ page import="org.apache.amber.oauth2.as.issuer.MD5Generator" %>
<%@ page import="org.apache.amber.oauth2.as.issuer.OAuthIssuer" %>
<%@ page import="org.apache.amber.oauth2.common.exception.OAuthProblemException" %>
<!DOCTYPE html>

<%
    // create an implementor...
    OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());

    // manage the lifecycle of the page here
    if (request.getMethod().equalsIgnoreCase("POST")) {
        // check if they came with oauth data and haven't already been handled
        // if they didn't come with oauth data or they're not in the process of being handled, reject them

        // check if they've authenticated already
    }
%>

<html>
    <head>
        <title>ohmage Data Access Confirmation</title>

        <!-- FIXME: no hardcoded links! -->
        <link rel="stylesheet" href="/app/oauth2/css/oauth.css" />
    </head>

    <body>
        <div id="trunk">
            <div class="panel">
                <h1>1. Log in to ohmage</h1>

                <div class="message">
                    <p>
                    In order to allow <b><%= request.getParameter("requesting_app")%></b> to
                    access your data on your behalf, you must first log in.
                    </p>

                    <p>
                    Fill in your ohmage username and password below and press "Log in" to proceed,
                    or close this window to cancel.
                    </p>
                </div>

                <form method="POST" action="/app/oauth/authorize">
                    <table class="vertical" cellpadding="0" cellspacing="0">
                        <tr>
                            <td class="label">Username:</td>
                            <td><input type="text" name="username" /></td>
                        </tr>

                        <tr>
                            <td class="label">Password:</td>
                            <td><input type="password" name="password" /></td>
                        </tr>

                        <tr>
                            <td colspan="2" align="right"><input type="submit" class="button" name="submit" value="Log In" /></td>
                        </tr>
                    </table>
                </form>
            </div>

            <div class="panel">
                <h1>2. Authorize <%= request.getParameter("requesting_app")%></h1>

                <div class="message">
                    <p>
                    Do you authorize <b><%= request.getParameter("requesting_app")%></b> to
                    access your ohmage data on your behalf?
                    </p>

                    <p>
                    (NOTE: at the moment there is no way to explicitly revoke this access should you decide
                    to do so, but this authorization will expire after 7 days.)
                    </p>
                </div>

                <form method="POST" action="/app/oauth/authorize">
                    <input type="submit" class="button" name="authorization_response" value="Confirm" />
                    <input type="submit" class="button" name="authorization_response" value="Deny" />
                </form>
            </div>
        </div>
    </body>
</html>