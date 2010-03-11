<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Welcome to AndWellness</title>
    
    <link href="/css/zp-compressed.css" type="text/css" media="screen, print" rel="stylesheet" />
	<link href="/css/zp-print.css" type="text/css" media="print" rel="stylesheet" />
	<link href="http://andwellness.cens.ucla.edu/favicon.ico" rel="shortcut icon" type="image/x-icon">
    
    <!--[if IE]>
	<link href="/css/zp-ie.css" type="text/css" media="screen" rel="stylesheet" />
	<![endif]-->
    
    <style type="text/css">
		
		.content .padding {
			padding: 50px 50px 50px 50px;
		}
		.footer {
		   color: #BEBEBE;
		}
	</style>
    
  </head>
  <body>
  
  <div class="zp-wrapper">
    <div class="zp-100 content">
		 <div class="padding">
  	
  		  <h1>Welcome to AndWellness.</h1>
  		  <h2>Please log in.</h2>
    
          <form method="post" action="/app/login">
		    <fieldset>

	          <c:if test="${sessionScope.failedLogin == true}">
			    <div class="notification error">You have entered an incorrect user name or password.</div>
			  </c:if>
			            				
		      <div class="form-item">
			    <label for="userName">User Name:</label>
				<input tabindex="1" id="userName" type="text" name="u" />
			  </div>
			  <div class="form-item">
				<label for="password">Password:</label>
				<input tabindex="2" id="password" type="password" name="p" />
			  </div>	
			  <button tabindex="3" id="send" type="submit">Send</button>
						
		    </fieldset>
		  </form>
        </div>
      </div>
    </div>
    
    <div class="zp-wrapper">
    <div class="zp-100 content">
      <div class="padding">
        <p class="footer">Question? Comment? Problem? Email us at andwellness-info@cens.ucla.edu.</p>
      </div>
    </div>
  </div>
  
  </body>
</html>
