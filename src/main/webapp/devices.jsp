<%@page import="javax.naming.NamingException"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="uk.ac.ed.epcc.MyResource"%>
<%@page import="java.sql.Connection"%>
<html>
<body>
    <h2>Registered devices</h2>
    <table>
    <thead>
    <tr><th align="left">Device</th><th align="left">Registration</th></tr>
    </thead>
    <tbody>
    <%
	Connection con = null;
	try{
		con = MyResource.getDataSource().getConnection();
		Statement statement = con.createStatement();
		String query = "SELECT HEX(uuid), FROM_UNIXTIME(timestamp/1000) FROM devices";
		ResultSet results = statement.executeQuery(query);
		while (results.next()) {
			String device = results.getString(1);
			String ts = results.getString(2);
			%>
			<tr>
				<td><a href="device_info.jsp?device=<%=device%>"><%=device%></a></td>
				<td><%=ts%></td>
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
