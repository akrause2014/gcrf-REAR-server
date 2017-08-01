package uk.ac.ed.epcc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
 * CREATE TABLE devices (uuid BINARY(16) NOT NULL, timestamp BIGINT, name VARCHAR(100), id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);
 * CREATE TABLE uploads (timestamp BIGINT, device INT NOT NULL, id SERIAL PRIMARY KEY, FOREIGN KEY (device) REFERENCES rear.devices(id), start BIGINT, end BIGINT, length BIGINT, system BIGINT, elapsed BIGINT, records BIGINT);
 * ALTER TABLE uploads ADD INDEX `device` (`device`);
 * ALTER TABLE devices ADD INDEX `uuid` (`uuid`);
 */

@Path("gcrf-REAR")
public class LocationResource {

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


	
}
