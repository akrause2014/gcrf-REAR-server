<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.naming.NamingException"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="uk.ac.ed.epcc.MyResource"%>
<%@page import="java.sql.Connection"%>


<html>
<body>
    <h2><%=request.getParameter("device")%></h2>
    <table>
    <thead>
    <tr><th align="left">Id</th><th align="left">Start (system time)</th><th>Length (ns)</th></tr>
    </thead>
    <tbody>
    <%
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
    String query = "SELECT Time.upload, MIN(Sensor.timestamp) AS start, MAX(Sensor.timestamp) AS end, systemTime FROM Time JOIN Sensor ON Time.upload=Sensor.upload GROUP BY Time.upload";
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
			String systemTime = dateFormat.format(new Date(st));
			long length = end-start;
			%>
			<tr>
				<td><%=upload%></td>
				<td><%=systemTime%></td>
				<td align="right"><%=length%></td>
				<td><a href="webapi/gcrf-REAR/data/<%=request.getParameter("device")%>/sensor/<%=upload%>">Download</a></td>
			</tr>
			<%
		}
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
