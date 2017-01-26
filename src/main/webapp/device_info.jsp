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
</head>
<body>
    <h2><%=request.getParameter("device")%></h2>
    <table>
    <thead>
    <tr>
    <th>Id</th>
    <th>Start (system time)</th>
    <th>End (system time)</th>
    <th>Length (ns)</th>
    <th></th>
    </tr>
    </thead>
    <tbody>
    <%
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    int device = RegisterDeviceResource.getDevice(request.getParameter("device"));
	String query = "SELECT Time.upload, MIN(S.timestamp) AS start, MAX(S.timestamp) AS end, systemTime " 
			+ "FROM Time JOIN " 
			+ "(SELECT upload, type, Sensor.timestamp, x, y, z FROM Sensor JOIN uploads "
					+ "ON id=upload WHERE device=" + device + ") as S " 
			+ "ON Time.upload=S.upload GROUP BY Time.upload";
	Connection con = null;
	try{
		con = MyResource.getDataSource().getConnection();
		Statement statement = con.createStatement();
		ResultSet results = statement.executeQuery(query);
		while (results.next()) {
			long upload = results.getLong(1);
			long start = results.getLong(2);
			long end = results.getLong(3);
			long st = results.getLong(4);
			long length = end-start;
			Date startDate = new Date(st);
			Date endDate = new Date(st+length/1000000); // length is in nanoseconds
			String systemTime = dateFormat.format(startDate);
			String endTime = dateFormat.format(endDate);
			String formattedLength = String.format("%,d", length);
			%>
			<tr>
				<td><%=upload%></td>
				<td><%=systemTime%></td>
				<td><%=endTime%></td>
				<td align="right"><%=formattedLength%></td>
				<td><a href="webapi/gcrf-REAR/data/<%=request.getParameter("device")%>/sensor/<%=upload%>">Download</a></td>
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
