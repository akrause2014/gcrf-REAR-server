<%@page import="uk.ac.ed.epcc.DataStoreResource"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.naming.NamingException"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="uk.ac.ed.epcc.RegisterDeviceResource"%>
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
   	    	$('#uploadTable').DataTable({
   	    		"order": [[ 1, "desc" ]],
   	    		"pageLength": 50,
   	    		stateSave: true
   	    	});
	    });
    </script>

</head>
<body>
    <h2><%=request.getParameter("device")%></h2>
    <table id="uploadTable" class="stripe row-border" cellspacing="0">
    <thead>
    <tr>
    <th>Id</th>
    <th>Start (system time)</th>
    <th>End (system time)</th>
    <th>Length (ms)</th>
    <th>Number of Records</th>
    <th></th>
    </tr>
    </thead>
    <tbody>
    <%
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    int device = RegisterDeviceResource.getDevice(request.getParameter("device"));
	String query = "SELECT id, system, elapsed, start, end, length, records FROM uploads WHERE device=" + device;
	Connection con = null;
	Statement statement = null;
	ResultSet results = null;
	try{
		con = DataStoreResource.getDataSource().getConnection();
		statement = con.createStatement();
		results = statement.executeQuery(query);
		while (results.next()) {
			long upload = results.getLong(1);
			long system = results.getLong(2);
			long elapsed = results.getLong(3);
			long start = results.getLong(4);
			long end = results.getLong(5);
			long length = results.getLong(6);
			long records = results.getLong(7);
			Date startDate = new Date(start);
			Date endDate = new Date(end); // length is in nanoseconds
			// difference between start timestamp and elapsed timestamp that matches the system time
			long diff = start/1000000-elapsed; 
			String systemTime = dateFormat.format(system + diff); 
			String endSystemTime = dateFormat.format(system+(length/1000000));
			String formattedLength = String.format("%.3f", ((double)length)/1000000000.0);
			String formattedRecords = String.format("%,d", records);
			%>
			<tr>
				<td><%=upload%></td>
				<td><%=systemTime%></td>
				<td><%=endSystemTime%></td>
				<td align="right"><%=formattedLength%></td>
				<td align="right"><%=formattedRecords%></td>
				<td><a href="webapi/gcrf-REAR/data/<%=request.getParameter("device")%>/<%=upload%>">Download</a></td>
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
