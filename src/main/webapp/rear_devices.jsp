<%@page import="java.util.Date"%>
<%@page import="java.util.TimeZone"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.io.File"%>
<%@page import="javax.naming.NamingException"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.Connection"%>
<%@page import="uk.ac.ed.epcc.DataStoreResource"%>

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
            $.fn.dataTable.moment( 'YYYY-MM-DD HH:mm:ss.SSS' );
               $('#deviceTable').DataTable({
                   "order": [[ 0, "asc" ]],
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
    <th align="left">ID</th>
    <th align="left">Device</th>
    <th align="left">Name</th>
    <th align="left">Registration</th>
    <th align="left">Last Upload</th>
    </tr>
    </thead>
    <tbody>
    <%
    Connection con = null;
    Statement statement = null;
    try{
        con = DataStoreResource.getDataSource().getConnection();
        statement = con.createStatement();
        String query = "SELECT HEX(uuid), name, timestamp, id FROM devices";
        ResultSet results = statement.executeQuery(query);
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        while (results.next()) {
            String device = results.getString(1);
            String name = results.getString(2);
            if (results.wasNull()) name = "";
            String ts = fmt.format(new Date(results.getLong(3)));
            int id = results.getInt(4);
            long lastModified = new File(DataStoreResource.DATA_DIR, String.valueOf(id)).lastModified();
            String lm = (lastModified > 0) ? fmt.format(new Date(lastModified)) : "";
            %>
            <tr>
                <td><%=id%></td>
                <td><a href="locations.jsp?device=<%=device%>"><%=device%></a></td>
                <td><%=name%></td>
                <td><%=ts%></td>
                <td><a href="webapi/gcrf-REAR/data/<%=device%>"><%=lm%></a></td>
            </tr>
            <%
        }
        results.close();
    } catch (SQLException e) {
    } catch (NamingException e) {
    }
    finally {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // ignore this 
            }
        }
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
