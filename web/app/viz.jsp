<%@ page contentType="text/html; charset=UTF-8" %>
<%@page import="edu.ucla.cens.awserver.domain.User"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-cache" );
   response.setDateHeader( "Expires", 0 );
%>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	
	<!-- Force IE8 into IE7 mode for VML compatibility -->
	<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
	
    <title>Data Visualizations</title>
    
    <link href="/css/zp-compressed.css" type="text/css" media="screen, print" rel="stylesheet" />
	<link href="/css/zp-print.css" type="text/css" media="print" rel="stylesheet" />
	<link href="http://andwellness.cens.ucla.edu/favicon.ico" rel="shortcut icon" type="image/x-icon">
	<link type="text/css" href="/css/jquery-ui-1.7.2.custom.css" rel="stylesheet" />
	<link type="text/css" href="/css/jquery-validity.css" rel="stylesheet" />
	<link type="text/css" href="/css/tabs.css" rel="stylesheet" />
    

	<!-- A large number of javascript includes, will reduce -->
	<!-- Main jQuery library -->
    <script type="text/javascript" src="/js/lib/jquery/jquery-1.4.2.min.js"></script>
	<!-- jQuery UI toolkit -->
	<script type="text/javascript" src="/js/lib/jquery/jquery.tools.min.js"></script>
	<!-- jQuery UI with Datepicker -->
	<script type="text/javascript" src="/js/lib/jquery/jquery-ui-1.7.2.custom.min.js"></script>
	<!-- Support logging -->
	<script type="text/javascript" src="/js/lib/misc/log4javascript.js"></script>
    <!-- Protovis graphing library with hacked in IE support -->
	<script type="text/javascript" src="/js/lib/Protovis/protovis-d3.1-ie.js"></script>
	<!-- Useful additions to Javascript objects -->
	<script type="text/javascript" src="/js/lib/misc/array_lib.js"></script>
	<script type="text/javascript" src="/js/lib/misc/date-functions.js"></script>
	<!-- Various validators to validate user input and server responses -->
	<script type="text/javascript" src="/js/lib/jquery/jquery.validity.min.js"></script>
	<script type="text/javascript" src="/js/lib/validator/DateValidator.js"></script>
	<!-- Generates the different graph types using Protovis -->
	<script type="text/javascript" src="/js/lib/DataSource/DataSource.js"></script>
	<script type="text/javascript" src="/js/lib/Graph/ProtoGraph.js"></script>
	<script type="text/javascript" src="/js/lib/DashBoard/DashBoard.js"></script>
	<!-- Contains the query response visualization types -->
	<script type="text/javascript" src="/js/response_list.js"></script>
    
	
    <!--[if IE]>
	<link href="/css/zp-ie.css" type="text/css" media="screen" rel="stylesheet" />
	<![endif]-->
    
    <style type="text/css">
		
		/* tab pane styling */
		div.panes > div {
		    display:none;       
		    padding:15px 10px;
		    border:1px solid #999;
		    border-top:0;
		    font-size:14px;
		    background-color:#fff;
		}
		
		ul.tabs {
		}
		
		body {
			margin: 0;
			padding: 0;
			background-color: #EEEEEE;
		}
		
		#wrapper{
			width:1000px;
		}

        #banner {
        	padding-left: 5px;
			padding-right: 5px;
			position: relative;
			background-color: white;
        }
		
		#main {
			padding-left: 5px;
			padding-right: 5px;
			margin-right:155px;
			background-color: white;
		}
		
		#logout {
			position: absolute;
            right: 5px;
			top: 0;
        }

		#controls {
			padding-right: 5px;
			float: right;
			width: 150px;
			background-color: white;
		}
		
		#footer {
			
		}
		
		#grabDateForm {
			font-size: .8em;
		}
		
		#grabDateForm .label {
            float: left;
            width: 60px;
			margin-right: 5px;
            text-align: right;
            font-weight: bold;
            clear: left;
        }
		
		#submit {
			margin-left: 65px;
			margin-top: 2px;
			background-color: #CBD893;
		}
		
		#startDate {
		    background-color: #FBEF99;
			width: 85px;
			height: 25px;
			margin-top: -2px;
		}
		
		#startDate:focus {
			background-color: #FDD041;
		}
		
		.loading {
			background-image: url('/css/images/ajax-loader.gif');
			background-repeat: no-repeat;
			background-position: center top;
			display: none;
			height: 75px;
		}
		.f {
		  font-family: Arial, sans-serif;
		}
		.h {
		  font-size: 36px;   	
	      line-height: 36px;
	      font-weight: normal;
		}
	</style>
    
	
	
	<script type="text/javascript">
	
	// Extend jQuery with a simple plugin called context to allow 
	// storage of context when making asynchronous calls from 
	// within an object.
    jQuery.extend({
      context: function(context) {
        var co = {
          callback: function(method) {
            if (typeof method == 'string') method = context[method];
            var cb = function() { 
			    method.apply(context, arguments); 
			};
            return cb;
          }
        };
        return co;
      }
    }); 
	
	
	// Holds the currently requested start date and number of days
	var startDate = new Date();
	var numDays = 0;

    // Holds the current page's DashBoard setup
	// Need to make a singleton
    var dashBoard = null;
	
	// Handles retrieval and filtering of data
	//var dataSource = new DataSourceJson('/app/viz');
		
	// Main logger
	var log = log4javascript.getLogger();
	
	// Called when document is done loading
    $(function() {
		// Setup logging
        var popUpAppender = new log4javascript.PopUpAppender();
        popUpAppender.setThreshold(log4javascript.Level.DEBUG);
		var popUpLayout = new log4javascript.PatternLayout("%d{HH:mm:ss} %-5p - %m%n");
        popUpAppender.setLayout(popUpLayout);
        log.addAppender(popUpAppender);

        // Uncomment the line below to disable logging
        //log4javascript.setEnabled(false);

		// Setup the datepickers for the date input box
		$("#startDate").datepicker({dateFormat: 'yy-mm-dd'});

	    // Override the default submit function for the form
	    $("#grabDateForm").submit(send_json_request);

		// Set initial start date to 2 weeks ago
		var today = new Date().incrementDay(-13).dateFormat('Y-m-d');
		$("#startDate").val(today);
		
		// Setup the dashboard
		log.debug("panes width is: " + $('.panes').width());
		dashBoard = new DashBoard(response_list, $('.panes').width());
		
		// Run the default query
		send_json_request(null);
	});

	/*
	 * Uses the validity library to validate the date form inputs on this page.
	 * Call this from the submit override before sending a request to the server.
	 */
    function validateDateFormInputs() {
    	// Start validation
        $.validity.start();

        // Validate the startDate field
        $("#startDate")
            .require("A starting date is required")
            .assert(DateValidator.validate, "Date is invalid.");

        // All of the validator methods have been called
        // End the validation session
        var result = $.validity.end();
        
        // Return whether it's okay to proceed with the request
        return result.valid;
    }

    /*
     * Grab the form inputs, validate, and send a request to the server for data.
     */
	function send_json_request(data) {
		// Grab the URL from the form
		var url = $("#grabDateForm").attr("action");
		var start_date = $("#startDate").val();
		var num_days = $("#numDays").val();

	    // Validate inputs
	    if (!validateDateFormInputs()) {
	        if (log.isWarnEnabled()) {
		        log.warn("Validation failed!");
	        }
	        return false;	    	 
	    }
		
		// Switch on the loading graphic
		dashBoard.loading(true);
		
		// Set global start and number of days
		startDate = Date.parseDate(start_date, "Y-m-d");
		numDays = parseInt(num_days);

        var end_date = startDate.incrementDay(numDays).dateFormat("Y-m-d");
		if (log.isInfoEnabled()) {
		    log.info("Grabbing data from " + start_date + " to " + end_date);
		}

		// Form the URL and send out
		url += "?s=" + start_date + "&e=" + end_date;     
        $.get(url, populate_graphs_with_json);

        if (log.isDebugEnabled()) {
            log.debug("Grabbing data from URL: " + url);
        }
        
		// Return false to cancel the usual submit functionality
		return false;
	}

	/*
	 * Iterate through each graph type, and add any data returned from the server
	 * to the graph.  Validate incoming JSON and make sure there were no server errors.
	 */
	function populate_graphs_with_json(json_data, text_status, http_request) {
        if (log.isInfoEnabled()) {
            log.info("Received JSON data from server with status: " + text_status);
        }		
		
	    // Did the request succeed?
	    if (text_status != "success") {
			if (log.isErrorEnabled()) {
			     log.error("Bad status from server: " + text_status);
		    }
			return;
	    }
		
		// Make sure we found JSON
		if (json_data == null || json_data.length == 0) {
			if (log.isWarnEnabled()) {
				log.warn("Bad response from server!");
			}
			
			// Turn off the loading graphic
			$(".loading").hide();
			return;
		}
		
	    // Run through possible error codes from server
		
		// 0104 is session expired, redirect to the passed URL
		if (json_data.error_code != null && json_data.error_code == "0104") {
			log.info("Session expired, redirecting to: " + json_data.error_text);
			
			window.location = json_data.error_text;
			return false;
		}
		
		// Make sure we have an array of data points
		if (!json_data instanceof Array) {
			if (log.isWarnEnabled()) {
                log.warn("No data found from server!");
            }
		}
		
		
		// DATA PREPROCESSING, MOVE TO SERVER OR IN TO A JS CLASS
		
		// Pull out the day into a Date for each data point
	    json_data.forEach(function(d) {
			var period = d.time.lastIndexOf('.');
	        d.date = Date.parseDate(d.time.substring(0, period), "Y-m-d g:i:s").grabDate();
			
			// Check if the date was parsed correctly
			if (d.date == null) {
				if (log.isErrorEnabled()) {
					log.error("Date parsed incorrectly from: " + d.time);
				}
			}
		});		
		
		// Load the data into the dashboard
		dashBoard.load_data(json_data);
		
		// Hide the loading graphic
		$(".loading").hide();
		// Reshow the graphs
		$(".ProtoGraph").show();
	}
	

	
    </script>
	
  </head>
  <body>
  <div id="wrapper">
  
  <!-- Get some CSS layout going here -->
  <div id="banner">
	<span class="f h">EMA Visualizations for <c:out value="${sessionScope.user.userName}"></c:out>.</span>
	<div id="logout"><a href="/app/logout">Logout</a></div>
  </div>
  
    <div id="controls" class="f">
    	Choose a time period:
		
        <form method="post" action="/app/viz" id="grabDateForm">
               <label for="startDate" class="label">Start Date:</label>
               <input id="startDate" type="text"/>
               <label for="numDays" class="label">Length:</label>
               <select id="numDays">
					<option value="7">1 week</option>
					<option selected="selected" value="14">2 weeks</option>
					<option value="21">3 weeks</option>
					<option value="28">4 weeks</option>
               </select>
             <button type="submit" id="submit">Go</button>
                    
         </form>
  </div>
  
  <div id="main" class="f">
	  <ul class="tabs"></ul> 
	  <div class="panes"></div>
  </dev>
  
  
 <div id="map" class="f">
 </div>
 
 <div id="footer" class="f">
 	Question? Comment? Problem? Email us at andwellness-info@cens.ucla.edu.
 </div>
  
  </div>
  </body>
</html>
