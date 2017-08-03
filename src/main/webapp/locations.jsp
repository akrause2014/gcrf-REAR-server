<%@page import="java.sql.Connection"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.SQLException"%>
<%@page import="uk.ac.ed.epcc.DataStoreResource"%>
<%@page import="uk.ac.ed.epcc.RegisterDeviceResource"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>

<%!
	public List<Double[]> getCoordinates(String device) {
		Connection con = null;
		Statement statement = null;
		ResultSet rs = null;
		int deviceId = RegisterDeviceResource.getDevice(device);
		List<Double[]> coordinates = new ArrayList<Double[]>();
		try {
		    con = DataStoreResource.getDataSource().getConnection();
		    statement = con.createStatement();
		    rs = statement.executeQuery("SELECT timestamp, provider, latitude, longitude, accuracy FROM location WHERE device=" + deviceId);
			while (rs.next()) {
				coordinates.add(new Double[] {rs.getDouble("latitude"), rs.getDouble("longitude")});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (rs != null)	rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (statement != null) statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return coordinates;
	}
%>

<html>
  <head>
    <title>Location <%=request.getParameter("device")%></title>
    <meta name="viewport" content="initial-scale=1.0">
    <meta charset="utf-8">
    <style>
      /* Always set the map height explicitly to define the size of the div
       * element that contains the map. */
      #map {
        height: 100%;
      }
      /* Optional: Makes the sample page fill the window. */
      html, body {
        height: 100%;
        margin: 20;
        padding: 20;
      }
    </style>
	<script language="javascript" type="text/javascript" src="flot/jquery.js"></script>
  </head>
  <body>
  	<h2><%=request.getParameter("device")%></h2>
  	<div id="info"></div>
    <div id="map"></div>
    <script>
    <%
List<Double[]> coordinates = getCoordinates(request.getParameter("device"));
if (coordinates.isEmpty()) {
	%>
	  $("#info").text("No locations found.");
	</script>
<%
}
else {
%>
      var map;
      function initMap() {
  	          map = new google.maps.Map(document.getElementById('map'), {
  	            zoom: 16
  	          });
	  	      var bound = new google.maps.LatLngBounds();
<%
	for (Double[] c : coordinates) {
		double lat = c[0];
		double lon = c[1];
%>
//	  		  $.each( data, function( key, val ) {
//	              var latLng = new google.maps.LatLng(val[2],val[3]);
				  var latLng = new google.maps.LatLng(<%=lat%>, <%=lon%>);
	              bound.extend(latLng);
	  	          var marker = new google.maps.Marker({
	  	            position: latLng,
	  	            map: map,
	  	            title: 'Timestamp'
	  	          });
//	  		  });
	          map.setCenter(bound.getCenter());
<%
	}
%>
 		  }
    </script>
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyDX_iK_LiYLcsyzO_u2A11JBzVlRtvTq0g&callback=initMap"
    async defer></script>
<% 
}
%>
  </body>
</html>
