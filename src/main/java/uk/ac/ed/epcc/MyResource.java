package uk.ac.ed.epcc;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import uk.ac.ed.epcc.rear.DataPoint;
import uk.ac.ed.epcc.rear.LocationDataPoint;
import uk.ac.ed.epcc.rear.SensorDataPoint;
import uk.ac.ed.epcc.rear.TimeDataPoint;

/**
 * CREATE TABLE devices (uuid BINARY(16), timestamp BIGINT, id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);
 * CREATE TABLE uploads (timestamp BIGINT, device INT NOT NULL, id SERIAL PRIMARY KEY, FOREIGN KEY (device) REFERENCES devices(id));
 * CREATE TABLE Sensor (upload BIGINT UNSIGNED NOT NULL, type INT, timestamp BIGINT, x REAL, y REAL, z REAL, FOREIGN KEY (upload) REFERENCES uploads(id));
 * CREATE TABLE Location (upload BIGINT UNSIGNED NOT NULL, timestamp BIGINT, latitude REAL, longitude REAL, altitude REAL, accuracy REAL, FOREIGN KEY (upload) REFERENCES uploads(id));
 * CREATE TABLE Time (upload BIGINT UNSIGNED NOT NULL, timestamp BIGINT, systemTime BIGINT, FOREIGN KEY (upload) REFERENCES uploads(id)); 
 * SELECT(uuid), FROM_UNIXTIME(timestamp/1000), id FROM devices;
 */

@Path("gcrf-REAR")
public class MyResource {
	
	public static final int VERSION = 1;
	
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
			System.out.println("Registered new device: " + id);
	    	return id;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		}
    }
    
    @Path("/register/{device}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String isRegistered(@PathParam("device") String device) {
		try {
    		Connection con = getDataSource().getConnection();
			getDeviceId(con, device);
		} catch (UnknownDeviceException e) {
			throw new NotFoundException("Unknown device: " + device);
		} catch (SQLException e) {
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			throw new ServerErrorException(500);
		}
        return "True";
    }
    
    @Path("/register/{device}")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public void deleteRegistered() {
    	// TODO
    }
    
    @Path("/data/{device}/time")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTimeData(@PathParam("device") String device) {
    	final int deviceId = getDevice(device);
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException {
    	    	Connection con = null;
    	    	try{
    	    		con = getDataSource().getConnection();
    	    		Statement statement = con.createStatement();
    	    		ResultSet results = statement.executeQuery(
    	    				"SELECT upload, " + TimeDataPoint.TABLE_NAME 
    	    				+ ".timestamp, systemTime FROM " 
    	    				+ TimeDataPoint.TABLE_NAME 
    	    				+ " JOIN uploads ON upload=id AND device=" + deviceId);
    	    		Writer writer = new BufferedWriter(new OutputStreamWriter(os));
    	    		while (results.next()) {
    	    			writer.write(String.format("%d,%d,%d\n", 
    	    					results.getInt(1),
    	    					results.getFloat(2), 
    	    					results.getFloat(3)));
    	    		}
    	    		writer.flush();
    	    		writer.close();

    	    	}
    	    	catch (SQLException e) {
    				e.printStackTrace();
    				throw new ServerErrorException(500);
    			} catch (NamingException e) {
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
    	  };
    	  return Response.ok(stream).build();
    }
    
    @Path("/data/{device}/sensor")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getSensorData(@PathParam("device") String device) {
    	final int deviceId = getDevice(device);
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException {
    	    	Connection con = null;
    	    	try{
    	    		con = getDataSource().getConnection();
    	    		Statement statement = con.createStatement();
    	    		ResultSet results = statement.executeQuery(
    	    				"SELECT upload, type, Sensor.timestamp, x, y, z FROM " + SensorDataPoint.TABLE_NAME 
    	    				+ " JOIN uploads ON upload=id AND device=" + deviceId);
    	    		Writer writer = new BufferedWriter(new OutputStreamWriter(os));
    	    		while (results.next()) {
        	    		String sensorType = "";
        	    		switch (results.getInt(2)) {
        	    		case DataPoint.SENSOR_TYPE_ACCELEROMETER:
        	    			sensorType = "A";
        	    			break;
        	    		case DataPoint.SENSOR_TYPE_GYROSCOPE:
        	    			sensorType = "G";
        	    			break;
        	    		case DataPoint.SENSOR_TYPE_MAGNETIC_FIELD:
        	    			sensorType = "M";
        	    			break;
        	    		}
    	    			writer.write(String.format("%d,%s,%d,%f,%f,%f\n", 
    	    					results.getInt(1), 
    	    					sensorType, 
    	    					results.getLong(3), 
    	    					results.getFloat(4), 
    	    					results.getFloat(5), 
    	    					results.getFloat(6)));
    	    		}
    	    		writer.flush();
    	    		writer.close();
    	    	}
    	    	catch (SQLException e) {
    				e.printStackTrace();
    				throw new ServerErrorException(500);
    			} catch (NamingException e) {
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
    	  };
    	  return Response.ok(stream).build();

    }
 
    @Path("/data/{device}/sensor")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void receiveData(
    		@PathParam("device") String device, 
    		InputStream is) 
    {
    	DataInputStream dataStream = new DataInputStream(is);
		int count = 0;
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
    		PreparedStatement statementSensor = con.prepareStatement(SensorDataPoint.getStatement());
    		PreparedStatement statementLocation = con.prepareStatement(LocationDataPoint.getStatement());
			System.out.println("Uploading data for device: " + device);
    		while (true) {
    			try {
		    		byte version = dataStream.readByte();
		    		if (version != VERSION) {
		    			new ServerErrorException("Unsupported message version: " + version, 400);
		    		}
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
		            case DataPoint.TYPE_LOCATION: {
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
		            case DataPoint.TYPE_TIME: {
		            	long systemTime = dataStream.readLong();
		            	TimeDataPoint dataPoint = new TimeDataPoint(uploadID, timestamp, systemTime);
		            	Statement statement = con.createStatement();
		            	statement.execute(dataPoint.getStatement());
		            	break;
		            }
		            default:
		            	// unsupported sensor type
		            	break;
		            }
		            count++;
    			}
    			catch (SQLException e) {
    				// log and move on to the next data point
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
			System.out.println("Upload complete for device: " + device + ", count=" + count);
    	}
    	
    }
    
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
   
    private int getDevice(String device)
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
    		if (con != null) {
    			try {
					con.close();
				} catch (SQLException e) {
					// ignore this 
				}
    		}
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
