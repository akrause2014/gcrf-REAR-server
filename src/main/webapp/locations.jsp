<%@page import="java.sql.Connection"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.SQLException"%>
<%@page import="uk.ac.ed.epcc.DataStoreResource"%>
<%@page import="uk.ac.ed.epcc.RegisterDeviceResource"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>


<%!
    public static List<Location> getCoordinates(String device) {
        Connection con = null;
        Statement statement = null;
        ResultSet rs = null;
        int deviceId = RegisterDeviceResource.getDevice(device);
        List<Location> coordinates = new ArrayList<Location>();
        try {
            con = DataStoreResource.getDataSource().getConnection();
            statement = con.createStatement();
            rs = statement.executeQuery("SELECT timestamp, provider, latitude, longitude, accuracy FROM location WHERE device=" + deviceId);
            while (rs.next()) {
                coordinates.add(new Location(
                        rs.getLong("timestamp"),
                        rs.getDouble("latitude"), 
                        rs.getDouble("longitude"),
                        rs.getFloat("accuracy"),
                        rs.getInt("provider")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (rs != null)    rs.close();
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
    
    public static class Location {
        public final double latitude;
        public final double longitude;
        public final Date timestamp;
        public final float accuracy;
        public final String provider;
        
        public Location(long timestamp, double lat, double lon, float acc, int prov) {
            longitude = lon;
            latitude = lat;
            this.timestamp = new Date(timestamp);
            accuracy = acc;
            switch (prov) {
            case 1: provider = "GPS"; break;
            case 2: provider = "network"; break;
            default: provider = "Unknown";
            }
        }
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
List<Location> coordinates = getCoordinates(request.getParameter("device"));
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
    for (Location l : coordinates) {
        double lat = l.latitude;
        double lon = l.longitude;
        DateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String title = fmt.format(l.timestamp) + ", accuracy: "+ l.accuracy + " provider: " + l.provider;
//        String title = "test";
%>
//                $.each( data, function( key, val ) {
//                  var latLng = new google.maps.LatLng(val[2],val[3]);
                  var latLng = new google.maps.LatLng(<%=lat%>, <%=lon%>);
                  var title = "<%=title%>";
                  bound.extend(latLng);
                    var marker = new google.maps.Marker({
                      position: latLng,
                      map: map,
                      title: title
                    });
//                });
<%
    }
%>
              google.maps.event.addListenerOnce(map, 'bounds_changed', function(event) {
                map.setZoom( Math.min( 16, map.getZoom() ) );
              });

              map.fitBounds(bound);
              map.setCenter(bound.getCenter());
           }
    </script>
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyDX_iK_LiYLcsyzO_u2A11JBzVlRtvTq0g&callback=initMap"
    async defer></script>
<% 
}
%>
  </body>
</html>
