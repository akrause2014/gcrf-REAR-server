
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
	<title>Uploads</title>
	<link href="flot/examples/examples.css" rel="stylesheet" type="text/css">
	<!--[if lte IE 8]><script language="javascript" type="text/javascript" src="../../excanvas.min.js"></script><![endif]-->
	<script language="javascript" type="text/javascript" src="flot/jquery.js"></script>
	<script language="javascript" type="text/javascript" src="flot/jquery.flot.js"></script>
	<script language="javascript" type="text/javascript" src="flot/jquery.flot.time.js"></script>
	<script language="javascript" type="text/javascript" src="flot/jquery.flot.selection.js"></script>
	<script type="text/javascript">

	options = {
			bars : {
				show: true,
				barWidth: 3600000
			},
			//lines: {
			//	show: true,
			//	lineWidth: 1
			//},
			xaxis: {
				mode: "time",
				timeformat:"%d/%m/%y %H:%M"
				//min: (new Date("2017/01/01")).getTime(),
			    //max: (new Date("2017/03/01")).getTime(),
				//tickSize: [1, "day"]
			},
			selection: {
				mode: "x"
			},
			grid: {
				hoverable: true,
				clickable: true
			},
		};
	var plot;
    var d = [];
    var dates = new Object();

    function getMetadata() {
		var url = "webapi/gcrf-REAR/metadata/" + "<%=request.getParameter("device")%>" + "/";
		return getMetadataFrom(url);
    }
    
	function getMetadataFrom(url, callback) {
		//console.log(url);
		d = [];
		dates = new Object();
		// id | start      | end        | length     | system        | elapsed    | records
		$.getJSON( url,	function( data ) {
		  $.each( data, function( key, val ) {
		    //console.log(key +"=" + val);
		    var start = val[1];
		    var system = val[4];
		    var elapsed = val[5];
		    var records = val[6];
		    var startTime = system + start/1000000 - elapsed;
		    var date = new Date(startTime);
		    date = new Date(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours());
		    var hour = date.getTime();
		    if (hour in dates) {
		    	dates[hour] = dates[hour]+records;
		    }
		    else {
		    	dates[hour] = records;
		    }
		  });
		  //console.log(dates);
		  $.each( dates, function( key, val ) {
			  d.push([key, val]);
		  });
		  if (callback) {
			  callback();
		  }
		  else {
	          plot = $.plot("#placeholder", [d], options);
		  }

		}).fail(function(xhr, status, error) {
				 console.log( "error: status=" + status + ", response: " + xhr.responseText );
		});

	}
	
	function setDateRange(s, t) {
		var si = s.indexOf("T");
		var ti = t.indexOf("T");
		$('#start_date').val(s.substring(0, si));
		// only get the full hour
		$('#start_time').val(s.substring(si+1, si+6));
		$('#end_date').val(t.substring(0, ti));
		$('#end_time').val(t.substring(ti+1, ti+6));
	}
	
	$(function() {
		getMetadata();

/* 		var overview = $.plot("#overview", [d], {
			series: {
				lines: {
					show: true,
					lineWidth: 1
				},
				shadowSize: 0
			},
			xaxis: {
				ticks: [],
				mode: "time"
			},
			yaxis: {
				ticks: [],
				min: 0,
				autoscaleMargin: 0.1
			},
			selection: {
				mode: "x"
			}
		});
 */
		$("#placeholder").bind("plotclick", function (event, pos, item) {
			if (item) {
				var timestamp = item.datapoint[0];
				//console.log(timestamp);
			}
	    }); 
		$("#placeholder").bind("plotselected", function (event, ranges) {

			// do the zooming
			$.each(plot.getXAxes(), function(_, axis) {
				var opts = axis.options;
				opts.min = ranges.xaxis.from;
				opts.max = ranges.xaxis.to;
			});
			
			var from = ranges.xaxis.from;
			var to = ranges.xaxis.to;
<%-- 			if (to-from < 3600000) {
				var fromDate = new Date(from);
				var url = "webapi/gcrf-REAR/metadata/" + "<%=request.getParameter("device")%>" + "/" 
							+ fromDate.getFullYear() + "/"
							+ (fromDate.getMonth()+1) + "/"
							+ fromDate.getDate() + "/" 
							+ fromDate.getHours() + "/";
				getMetadataFrom(url, function() {
					var records = 0;
					$.each( dates, function( key, val ) {
						if (key >= from && key < to) {
							records += val;
						}
					});
					$('#selected_records').text('Selected: ' + records.toLocaleString() + " records");
					plot.setupGrid();
					plot.draw();
					plot.clearSelection();
				});
			}
			else {
 --%>				plot.setupGrid();
				plot.draw();
				plot.clearSelection();
				var records = 0;
				$.each( dates, function( key, val ) {
					if (key >= from && key < to) {
						records += val;
					}
				});
				$('#selected_records').text('Selected: ' + records.toLocaleString() + " records");
			//}
			
			$('#download').prop('disabled', false);
			
			var s = new Date(from).toISOString();
			var t = new Date(to).toISOString();
			setDateRange(s, t);

			// don't fire event on the overview to prevent eternal loop
			//overview.setSelection(ranges, true);
		});
		
		$('#download').bind("click", function() {
			var startDate = $('#start_date').val();
			var startTime = $('#start_time').val();
			var endDate = $('#end_date').val();
			var endTime = $('#end_time').val();
			if (startDate) {
				if (endDate) {
					if (!startTime) startTime = '00:00';
					if (!endTime) endTime = '00:00';
					var s = new Date(startDate + "T" + startTime);
					var e = new Date(endDate + "T" + endTime);
					var url = "webapi/gcrf-REAR/data/" + "<%=request.getParameter("device")%>" 
						+ "/date/"+ startDate + "T" + startTime + "/" + endDate + "T" + endTime;
					console.log(url);
					$(location).attr('href',url);
				}
			}
		});
		
		$('#clear_selection').bind("click", function() {
			$.each(plot.getXAxes(), function(_, axis) {
				var opts = axis.options;
				delete opts.min;
				delete opts.max;
			});
			$('#start_date').val('');
			// only get the full hour
			$('#start_time').val('');
			$('#end_date').val('');
			$('#end_time').val('');
			$('#selected_records').text('');
			$('#download').prop('disabled', true);
			plot.setupGrid();
			plot.draw();
			plot.clearSelection();
		});

//		$("#overview").bind("plotselected", function (event, ranges) {
//			plot.setSelection(ranges);
//		});

	});

	</script>
</head>
<body>

	<div id="header">
		<h3>Device <%=request.getParameter("device")%></h3>
	</div>

	<div id="content">
	
		<div>This chart shows the number of records uploaded from this device.</div>

		<div class="demo-container">
			<div id="placeholder" class="demo-placeholder"></div>
		</div>

		<div>		
			<label for="start_date">Start Date:</label>
			<input id="start_date" type="date"></input>
			<label for="start_time">Time:</label>
			<input id="start_time" type="time"></input>
		</div>
		<div>
			<label for="end_date">End Date:</label>
			<input id="end_date" type="date"></input>
			<label for="end_time">Time:</label>
			<input id="end_time" type="time"></input>
		</div>
		<div>
			<div id="selected_records"></div>
			<button id="download" disabled="disabled">Download</button>
		</div>
		<div>
			<button id="clear_selection">Clear Selection</button>
		</div>
		
<!-- 		<div class="demo-container" style="height:150px;">
			<div id="overview" class="demo-placeholder"></div>
		</div>
 -->
 
 		<div>
 		<a href="rear_device_info.jsp?device=<%=request.getParameter("device")%>">View list of all uploads</a>
 		</div>
	</div>

</body>
</html>
