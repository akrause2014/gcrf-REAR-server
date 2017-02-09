<%@page import="javax.naming.NamingException"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="uk.ac.ed.epcc.MyResource"%>
<%@page import="java.sql.Connection"%>
<html>
<head>
	<link rel="stylesheet" type="text/css" href="//cdn.datatables.net/1.10.13/css/jquery.dataTables.css">
	<script type="text/javascript" type="text/javascript" src="flot/jquery.js"></script>
	<script type="text/javascript" charset="utf8" src="//cdn.datatables.net/1.10.13/js/jquery.dataTables.js"></script>
	<script type="text/javascript" charset="utf8" src="//cdnjs.cloudflare.com/ajax/libs/moment.js/2.8.4/moment.min.js"></script>
	<script type="text/javascript" charset="utf8" src="//cdn.datatables.net/plug-ins/1.10.13/sorting/datetime-moment.js"></script>

    <style>
    table {
        border-collapse: collapse;
    }
    
    table, th, td {
        border: 1px solid black;
    }
    th, td {
        padding: 10px;
    }
    </style>
    <script type="text/javascript">
	    $(document).ready(function(){
	        $.fn.dataTable.moment( 'DD/MM/YYYY HH:mm:ss.SSS' );
   	    	$('#deviceTable').DataTable({
   	    		"order": [[ 2, "asc" ]],
   	    		"pageLength": 50,
   	    		stateSave: true
   	    	});
	    });
    </script>
    
</head>
<body>
    <h2>Registered devices</h2>
    <table id="deviceTable">
    <thead>
    <tr>
    <th align="left">Device</th>
    <th align="left">Name</th>
	<th align="left">Registration</th>
	</tr>
    </thead>
    <tbody>
    <%
    Connection con = null;
    try{
        con = MyResource.getDataSource().getConnection();
        Statement statement = con.createStatement();
        String query = "SELECT HEX(uuid), name, FROM_UNIXTIME(timestamp/1000) FROM devices";
        ResultSet results = statement.executeQuery(query);
        while (results.next()) {
            String device = results.getString(1);
            String name = results.getString(2);
            if (results.wasNull()) name = "";
            String ts = results.getString(3);
            %>
            <tr>
                <td><a href="device_uploads.jsp?device=<%=device%>"><%=device%></a></td>
                <td><%=name%></td>
                <td><%=ts%></td>
            </tr>
            <%
        }
        results.close();
        statement.close();
    } catch (SQLException e) {
    } catch (NamingException e) {
    }
    finally {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                // ignore this 
            }
        }
    }
    %>
    </tbody>
    </table>
</body>
</html>
