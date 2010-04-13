<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Welcome to AndWellness</title>
    
    <link href="/css/zp-compressed.css" type="text/css" media="screen, print" rel="stylesheet" />
	<link href="/css/zp-print.css" type="text/css" media="print" rel="stylesheet" />
	<link href="http://andwellness.cens.ucla.edu/favicon.ico" rel="shortcut icon" type="image/x-icon">
    <!-- Custom CSS for the "dashboard" setup -->
    <link type="text/css" href="/css/dashboard.css" rel="stylesheet" />
    
    <!--[if IE]>
	<link href="/css/zp-ie.css" type="text/css" media="screen" rel="stylesheet" />
	<![endif]-->
    
    <style type="text/css">
		
		.content .padding {
			padding: 50px;
		}
		.p-bottom {
			padding-bottom: 10px; 
		}
		.f {
		  font-family: Arial, sans-serif;
		}
		.h {
		  font-size: 36px;   	
	      line-height: 36px;
	      font-weight: normal;
		}
        .t {
          font-size: 24 px;
          font-weight: normal;
        }
	</style>
    
  </head>
  <body>
  
    
  <!-- Wrap the entire page in a custom div, maybe can use body instead -->
  <div id="wrapper" class="f">
  
  <!-- Dashboard banner -->
  <div id="banner">
    <span class="h">Welcome to AndWellness.</span>
  </div>
  
  <div id="controls">
    <form method="post" action="/app/login" id="loginForm">
      <c:if test="${sessionScope.failedLogin == true}">
        <div class="notification error">You have entered an incorrect user name or password.</div>
      </c:if>
    
      <label for="userName" class="label">User Name:</label>
      <input id="userName" type="text" name="u"/>
        
      <label for="password" class="label">Password:</label>
      <input id="password" type="password" name="p"/>
      
      <button type="submit" id="submit">Send</button>              
    </form>
  </div>
  
  <!-- Main body of the dashboard -->
  <div id="main">
  
    <h2>AndWellness</h2>
    <p>AndWellness is a mobile and web application that makes it easy to collect,
    manage, and visualize data.</p>  

    <h3>Configure</h3>
    <p>Something about configuration</p>
    
    
    <h3>Deploy</h3>
    <p>Something about deployation</p>
    
    <h3>Visualize</h3>
    <p>Something about visualization</p>
  </div>
  
  <!-- Dashboard footer -->
  <div id="footer">
    Question? Comment? Problem? Email us at andwellness-info@cens.ucla.edu.
  </div>
  
  </div>
  </body>
</html>
