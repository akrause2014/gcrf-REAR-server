package uk.ac.ed.epcc;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.naming.NamingException;
import javax.ws.rs.Consumes;
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
import uk.ac.ed.epcc.rear.DataReader;
import uk.ac.ed.epcc.rear.DataVisitor;

/**
 * CREATE TABLE location (timestamp BIGINT, device INT NOT NULL, provider INT, latitude DOUBLE, longitude DOUBLE, accuracy FLOAT, id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, FOREIGN KEY (device) REFERENCES rear.devices(id));
 * 
 * curl -X POST -H "Content-Type: application/octet-stream" --data-binary @/tmp/locationtest http://localhost:8080/gcrfREAR/webapi/gcrf-REAR/location/CF3A75A8AC204DDEA95427C65DD51929/
 */

@Path("gcrf-REAR")
public class LocationResource {
	
	@Path("/location/{device}") 
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public void postLocation(
			@PathParam("device") String device, 
			InputStream is) 
	{
//		System.out.println("RECEIVING LOCATION DATA");
		int deviceId = RegisterDeviceResource.getDevice(device);
		Connection con = null;
		PreparedStatement statement = null;
    	try {
    		DataInputStream ds = new DataInputStream(is);
    		con = DataStoreResource.getDataSource().getConnection();
    		statement = con.prepareStatement(
    				"INSERT INTO location (timestamp, device, provider, latitude, longitude, accuracy) VALUES (?,?,?,?,?,?)");
    		while (true) {
    			long timestamp = ds.readLong();
    			int provider = ds.readInt();
    			double latitude = ds.readDouble();
    			double longitude = ds.readDouble();
    			float accuracy = ds.readFloat();
    			statement.setLong(1, timestamp);
    			statement.setInt(2, deviceId);
    			statement.setInt(3, provider);
    			statement.setDouble(4, latitude);
    			statement.setDouble(5, longitude);
    			statement.setFloat(6, accuracy);
    			int result = statement.executeUpdate();
    		}
		} catch (EOFException e) {
			// end of file
		} catch (SQLException e) {
			throw new ServerErrorException("Server error", 500, e);
		} catch (NamingException e) {
			throw new ServerErrorException("Server error", 500, e);
		} catch (IOException e) {
			throw new ServerErrorException("Server error", 500, e);
		}
    	finally {
			try {
	    		if (statement != null) statement.close();
			} catch (SQLException e) {
			}
			try {
	    		if (con != null) con.close();
			} catch (SQLException e) {
			}
    	}

	}

    @Path("/location/{device}/uploads/{upload}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response serveMetadata(
    		@PathParam("device") String device,
    		@PathParam("upload") String upload) 
    {
   		return serveMetadataBetween(device, upload, upload);
    }
	
    @Path("/location/{device}/uploads/{from}/{to}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response serveMetadataBetween(
    		@PathParam("device") String device,
    		@PathParam("from") String from,
    		@PathParam("to") String to) 
    {
    	int deviceId = RegisterDeviceResource.getDevice(device);
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException 
    	    {
    	    	Writer writer = new OutputStreamWriter(os);
    	    	writer.write("[\n");
    	    	Connection con = null;
    	    	Statement statement = null;
    	    	ResultSet rs = null;
    	    	try {
    				con = DataStoreResource.getDataSource().getConnection();
    				statement = con.createStatement();
    				rs = statement.executeQuery("SELECT id, system, elapsed FROM uploads WHERE id BETWEEN " + from + " AND " + to + " AND device=" + deviceId);
    				boolean isFirst = true;
    				while (rs.next()) {
    					int upload = rs.getInt(1);
    					long systemTime = rs.getLong(2);
    					long elapsedTime = rs.getLong(3);
	    	    		File file = DataStoreResource.getFile(deviceId, upload);
	    	    		LocationVisitor visitor = new LocationVisitor(writer, isFirst);
	    	    		new DataReader().read(file, visitor);
	    	    		isFirst = false;
	    	    	}
					con.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			} catch (NamingException e) {
    				e.printStackTrace();
    			} catch (FileNotFoundException e) {
    				System.err.println("File not found: " + e.getMessage());
    				throw new NotFoundException();
    			} catch (IOException e) {
    				throw new ServerErrorException("Server error", 500, e);
    			}
    	    	finally {
					try {
	    	    		if (rs != null)	rs.close();
					} catch (SQLException e) {
					}
					try {
	    	    		if (statement != null) statement.close();
					} catch (SQLException e) {
					}
					try {
	    	    		if (con != null) con.close();
					} catch (SQLException e) {
					}
    	    	}
				writer.write("]");
    	    	writer.flush();
    	    	writer.close();
    	    }
    	};
    	return Response.ok(stream).build();
    }
    
 	class LocationVisitor implements DataVisitor {
		
		private Writer writer;
		private boolean isFirst;

		public LocationVisitor(Writer writer, boolean isFirst) {
			this.writer = writer;
			this.isFirst = isFirst;
		}
		
		@Override
		public void visitTime(long timestamp, long systemTime) {
			// ignore
		}
		
		@Override
		public void visitSensor(long timestamp, int sensorType, float x, float y, float z) {
			// ignore
		}
		
		@Override
		public void visitLocation(long timestamp, int sensorType, 
				double latitude, double longitude, double altitude, float accuracy) {
        	try {
        		if (!isFirst) {
        			writer.write(",");
        		}
        		else {
        			isFirst = false;
        		}
        		String provider;
        		switch (sensorType) {
        		case DataPoint.TYPE_LOCATION: provider = "gps"; break;
        		case DataPoint.TYPE_LOCATION_NETWORK: provider = "network"; break;
        		default: provider = "";
        		}
				writer.write(String.format("[%d,\"%s\",%f,%f,%f,%f]\n", timestamp, provider, latitude, longitude, altitude, accuracy));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void visitEndOfFile() {
			try {
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	public static List<Location> getCoordinates(String device) {
		Connection con = null;
		Statement statement = null;
		ResultSet rs = null;
		int deviceId = RegisterDeviceResource.getDevice(device);
		List<Location> coordinates = new ArrayList<>();
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
				if (rs != null)	rs.close();
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

	
}
