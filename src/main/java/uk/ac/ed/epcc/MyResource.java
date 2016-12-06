package uk.ac.ed.epcc;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;

import uk.ac.ed.epcc.rear.DataPoint;
import uk.ac.ed.epcc.rear.LocationDataPoint;
import uk.ac.ed.epcc.rear.SensorDataPoint;

/**
 * CREATE TABLE devices (uuid BINARY(16), timestamp BIGINT, id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);
 * CREATE TABLE uploads (timestamp BIGINT, device INT NOT NULL, id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, FOREIGN KEY (device) REFERENCES devices(id));
 * CREATE TABLE Sensor (upload INT NOT NULL, type INT, timestamp BIGINT, x REAL, y REAL, z REAL, FOREIGN KEY (upload) REFERENCES uploads(id));
 * CREATE TABLE Location (upload INT NOT NULL, timestamp BIGINT, latitude REAL, longitude REAL, altitude REAL, accuracy REAL, FOREIGN KEY (upload) REFERENCES uploads(id));
 *  
 * SELECT(uuid), FROM_UNIXTIME(timestamp/1000), id FROM devices;
 */

@Path("gcrf-REAR")
public class MyResource {
	
    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Got it!";
    }
    
    @Path("/register")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String registerDevice() {
    	long currentTime = System.currentTimeMillis();
    	UUID uuid = UUID.randomUUID();
    	String id = uuid.toString().replaceAll("-", "");
    	try {
			Connection connection = getDataSource().getConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("INSERT INTO devices (uuid, timestamp) VALUES (UNHEX(\"" + id + "\"), " + currentTime + ")");
			System.out.println("REGISTERED DEVICE " + id);
	    	return id;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		}
    }
    
    @Path("/data/{device}/sensor")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void receiveData(
    		@PathParam("device") String device, 
    		InputStream is) {
    	DataInputStream dataStream = new DataInputStream(is);
    	Connection con = null;
    	try {
    		con = getDataSource().getConnection();
    		int deviceId;
			try {
				deviceId = getDeviceId(con, device);
			} catch (UnknownDeviceException e) {
				throw new NotFoundException("Unknown device: " + device);
			}
			long uploadTs = System.currentTimeMillis();
			int uploadID = createUpload(con, uploadTs, deviceId);
			System.out.println("UPLOADING DATA FOR DEVICE " + device);
    		PreparedStatement statementSensor = con.prepareStatement(SensorDataPoint.getStatement());
    		PreparedStatement statementLocation = con.prepareStatement(LocationDataPoint.getStatement());
    		while (true) {
    			try {
		    		byte version = dataStream.readByte();
		            int sensorType = dataStream.readByte();
		            long timestamp = dataStream.readLong();
		            switch (sensorType) {
		            case DataPoint.SENSOR_TYPE_ACCELEROMETER:
		            case DataPoint.SENSOR_TYPE_GYROSCOPE:
		            case DataPoint.SENSOR_TYPE_MAGNETIC_FIELD: {
		                float x = dataStream.readFloat();
		                float y = dataStream.readFloat();
		                float z = dataStream.readFloat();
		                SensorDataPoint dataPoint = new SensorDataPoint(uploadID, sensorType, timestamp, x, y, z);
		                dataPoint.prepareStatement(statementSensor);
		                statementSensor.execute();
		            	break;
		            }
		            case DataPoint.SENSOR_TYPE_LOCATION: {
		            	double latitude = dataStream.readDouble();
		            	double longitude = dataStream.readDouble();
		            	double altitude = dataStream.readDouble();
		            	float accuracy = dataStream.readFloat();
		            	LocationDataPoint dataPoint = 
		            			new LocationDataPoint(uploadID, sensorType, timestamp, latitude, longitude, altitude, accuracy);
		                dataPoint.prepareStatement(statementLocation);
		                statementLocation.execute();
		            	break;
		            }
		            default:
		            	// unsupported sensor type
		            	break;
		            }
    			}
    			catch (SQLException e) {
    				e.printStackTrace();
    			}
    		}
    	}
    	catch (EOFException e) {
    		//finished
    	}
    	catch (IOException e) {
    		e.printStackTrace();
			throw new ServerErrorException(500);
    	}
    	catch (SQLException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ServerErrorException(500);
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
    	
    }
    
//    CREATE TABLE devices (uuid BINARY(16), timestamp BIGINT, id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);
    private int getDeviceId(Connection con, String device) throws SQLException, UnknownDeviceException 
    {
		Statement query = con.createStatement();
		try {
			ResultSet result = query.executeQuery("SELECT id FROM devices WHERE HEX(uuid)=\"" + device + "\"");
			if (result.next()) {
				return result.getInt(1);
			}
			else {
				throw new UnknownDeviceException(device);
			}
		}
		finally {
			query.close();
		}
    }
    
    private int createUpload(Connection con, long uploadTs, int deviceId) throws SQLException {
		Statement s = con.createStatement();
		s.executeUpdate("INSERT INTO uploads (timestamp, device) VALUES (" + uploadTs + "," + deviceId + ")");
		ResultSet result = s.executeQuery("SELECT id FROM uploads WHERE timestamp=" + uploadTs);
		if (result.next()) {
			return result.getInt(1);
		}
		else {
			throw new ServerErrorException(500);
		}
    }
    
	private DataSource getDataSource() throws NamingException
	{
		InitialContext cxt = new InitialContext();
		DataSource ds = (DataSource) cxt.lookup( "java:/comp/env/jdbc/rear_db" );
		return ds;
	}

}
