package uk.ac.ed.epcc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;

/**
 * CREATE TABLE devices (uuid BINARY(16) NOT NULL, timestamp BIGINT, name VARCHAR(100), id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);
 * 
 * SELECT(uuid), FROM_UNIXTIME(timestamp/1000), id FROM devices;
 */

@Path("gcrf-REAR")
public class RegisterDeviceResource {
	
    @Path("/register")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String registerDeviceForm(
    		@FormParam("name") String name)
    {
    	try {
	    	return registerDevice(URLDecoder.decode(name, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new ServerErrorException(500);
		}
    }

	
    @Path("/register")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String registerDevice(
    		@DefaultValue("") @QueryParam("name") String name)
    {
    	long currentTime = System.currentTimeMillis();
    	UUID uuid = UUID.randomUUID();
    	String id = uuid.toString().replaceAll("-", "");
    	Connection connection = null;
    	Statement statement = null;
    	try {
			connection = getDataSource().getConnection();
			statement = connection.createStatement();
			String sql;
			if (name != null && !name.isEmpty()) {
				sql = "INSERT INTO devices (uuid, timestamp, name) VALUES (UNHEX(\"" + id + "\"), " + currentTime + ", \"" + name + "\")";
			}
			else {
				sql = "INSERT INTO devices (uuid, timestamp) VALUES (UNHEX(\"" + id + "\"), " + currentTime + ")";
			}
			statement.executeUpdate(sql);
			System.out.println("Registered new device: " + id + ", name = " + name);
	    	return id;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		}
    	finally {
    		closeStatement(statement);
    		closeConnection(connection);
    	}
    }
    
    @Path("/register/{device}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String isRegistered(@PathParam("device") String device) {
    	getDevice(device);
        return "True";
    }
    
    @Path("/register/{device}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public void deleteRegistered() {
    	// TODO
    }
    
    public static int getDeviceId(Connection con, String device) throws SQLException, UnknownDeviceException 
    {
		Statement query = null;
		ResultSet result = null;
		try {
			query = con.createStatement();
			result = query.executeQuery("SELECT id FROM devices WHERE HEX(uuid)=\"" + device + "\"");
			if (result.next()) {
				return result.getInt(1);
			}
			else {
				throw new UnknownDeviceException(device);
			}
		}
		finally {
			if (result != null) result.close();
			if (query != null) query.close();
		}
    }
   
    public static String getDeviceByName(Connection con, String name) throws SQLException, UnknownDeviceException 
    {
		Statement query = null;
		ResultSet result = null;
		try {
			query = con.createStatement();
			result = query.executeQuery("SELECT name FROM devices WHERE name=\"" + name + "\"");
			if (result.next()) {
				// using the first device in the database
				// assumes that the device name is unique... 
				return result.getString(1);
			}
			else {
				throw new UnknownDeviceException(name);
			}
		}
		finally {
			if (result != null) result.close();
			if (query != null) query.close();
		}
    }
    
    public static String getDeviceByName(String name)
    {
    	Connection con = null;
    	try {
    		con = getDataSource().getConnection();
        	try {
        		return getDeviceByName(con, name);
        	} catch (UnknownDeviceException e) {
        		throw new NotFoundException("Unknown device: " + name);
        	}
    	}
    	catch (SQLException e) {
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			throw new ServerErrorException(500);
		}
    	finally {
    		closeConnection(con);
    	}
    }


   
    public static int getDevice(String device)
    {
    	Connection con = null;
    	try {
    		con = getDataSource().getConnection();
        	try {
        		return getDeviceId(con, device);
        	} catch (UnknownDeviceException e) {
        		throw new NotFoundException("Unknown device: " + device);
        	}
    	}
    	catch (SQLException e) {
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			throw new ServerErrorException(500);
		}
    	finally {
    		closeConnection(con);
    	}
    }


	public static DataSource getDataSource() throws NamingException
	{
		InitialContext cxt = new InitialContext();
		DataSource ds = (DataSource) cxt.lookup( "java:/comp/env/jdbc/rear_db" );
		return ds;
	}
	
	private static void closeStatement(Statement statement) {
		try {
			if (statement != null) statement.close();
		} catch (SQLException e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}
	
	public static void closeConnection(Connection connection) {
		try {
			if (connection != null) connection.close();
		} catch (SQLException e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

}
