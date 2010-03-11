/*
 * DashBoard handles the HTML and CSS setup of a users' dashboard.
 * The DashBoard reads in a local or remote JSON configuration, and
 * loads a dashboard based on the contents of the configuration.
 */

// DashBoard consturctor - For now the config is read locally and
// is simply a list of question types.  The graph_width is the total
// width in pixels the graphs will take.
function DashBoard(json_config, graph_width) {
    // Configure the HTML and CSS for the dashboard
    this.configure_html(json_config, graph_width);
}

// Logger for the dashboard
DashBoard._logger = log4javascript.getLogger();

// Dashboard functions

// Setup the HTML and CSS according to the configuration JSON.
DashBoard.prototype.configure_html = function(json_config, graph_width) {
    var cur_group = -1;
    
    // Loop over each graph type
    json_config.forEach(function(config) {
        // If we are in a new group, add a new pane to the tabs
        if (cur_group != config.group_id) {
            // Grab new group name from the group_list
            var new_group_name = group_list[config.group_id];
            // Translate the name into something that works as an html reference
            var new_group_name_ref = new_group_name.toLowerCase().replace(' ', '_');
            
            if (DashBoard._logger.isDebugEnabled()) {
                DashBoard._logger.debug("Creating group name: " + new_group_name + " with ref: " + new_group_name_ref);
            }
            
            $('.tabs').append('<li><a href="' + new_group_name_ref + '">' + new_group_name + '</a></li>');
            $('.panes').append('<div id="group_' + config.group_id + '"></div>');
            
            cur_group = config.group_id;
        }
        
        // Now append a new div into the panes for our new graph
        $('.panes > #group_' + cur_group).append('<div class="ProtoGraph" id="prompt_' + config.prompt_id + '"></div>');
    
        // Create a unique div ID for Protovis to know where to attach the graph
        var div_id = 'ProtoGraph_' + cur_group + '_' + config.prompt_id;
        
        // Put the graph title and another div for the graph itself into this div
        $('#group_' + cur_group + ' > #prompt_' + config.prompt_id)
            .append('<h3>' + config.text + '</h3>')
            .append('<div id="' + div_id + '"></div>');
        
        // Finally create a new graph and add it to the div
        var new_graph = ProtoGraph.factory(config, div_id, graph_width);
        $('#' + div_id)
            .data('graph', new_graph)
            .data('prompt_id', config.prompt_id)
            .data('group_id', cur_group);
    });
    
    // setup ul.tabs to work as tabs for each div directly under div.panes 
    $("ul.tabs").tabs("div.panes > div");
    
    // Hide all the graphs for now
    $('div.ProtoGraph').hide();
    
    // Append a loading div in the pane
    $('div.panes > div').append('<div class="loading"></div>');
}

// Load new data into the graphs and rerender
DashBoard.prototype.load_data = function(json_data) {
    var data = json_data;
    
    // iterate over every ProtoGraph class
    $('div.ProtoGraph > div').each(function(index) {
        // Grab the graph object attached to this div
        var graph = $(this).data('graph');
        var prompt_id = $(this).data('prompt_id');
        var group_id = $(this).data('group_id');
        
        if (log.isDebugEnabled()) {
            log.debug("Rendering graph with prompt_id " + prompt_id + " group_id " + group_id);
            var start_render_time = new Date().getTime();
        }
        
        // Also filter out SKIPPED points for now
        var new_data = data.filter(function(data_point) {
            return ((prompt_id == data_point.prompt_id) && 
                    (group_id == data_point.prompt_group_id) &&
                    (data_point.response != "RESPONSE_SKIPPED"));
        });
        
        // Check if any data was found
        if (new_data.length == 0) {
            if (log.isInfoEnabled()) {
                log.info("No data found for group_id " + group_id + " prompt_id " + prompt_id);
            }
        }
        
        // Apply data to the graph
        graph.apply_data(new_data, 
                         startDate, 
                         numDays);
                         
        // Re-render graph with the new data
        graph.render();
        
        if (log.isDebugEnabled()) {
            var time_to_render = new Date().getTime() - start_render_time;
            
            log.debug("Time to render graph: " + time_to_render + " ms");
        }               
    });
}

// Enable/disable the loading graphic
DashBoard.prototype.loading = function(enable) {
    if (enable) {
        // Show the loading graphic in the displayed pane
        $('div.panes .loading').show();
    }
    else {
        // Hide all the loading divs in the panes
        $('div.panes .loading').hide();
    }
}

