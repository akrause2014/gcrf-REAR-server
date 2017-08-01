<!DOCTYPE html>
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
      var map;
      function initMap() {
        var url = "webapi/gcrf-REAR/location/" + "<%=request.getParameter("device")%>" +  "/uploads/" + "<%=request.getParameter("upload")%>";
  		$.getJSON( url,	function( data ) {
  		  var bound = new google.maps.LatLngBounds();
  		  if (jQuery.isEmptyObject(data)) {
  			  $("#info").text("No locations found.");
  		  }
  		  else {
  	          map = new google.maps.Map(document.getElementById('map'), {
  	            zoom: 16
  	          });
	  		  $.each( data, function( key, val ) {
	              var latLng = new google.maps.LatLng(val[2],val[3]);
	              bound.extend(latLng);
	  	          var marker = new google.maps.Marker({
	  	            position: latLng,
	  	            map: map
	  	          });
	  		  });
	          map.setCenter(bound.getCenter());
  		  }
  		}).fail(function() {
			  $("#info").text("No locations found.");
  		});
      }
    </script>
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyDX_iK_LiYLcsyzO_u2A11JBzVlRtvTq0g&callback=initMap"
    async defer></script>
  </body>
</html>
