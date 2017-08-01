package uk.ac.ed.epcc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.naming.NamingException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import uk.ac.ed.epcc.rear.DataVisitor;

@Path("gcrf-REAR")
public class DownloadResource {
	
	public static final int VERSION = 1;
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss.SSSZ");

	@Path("/data/query")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getTimeData(@Context UriInfo uriInfo) {
    	MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
    	List<String> devices = params.get("name");
    	String start = params.getFirst("starttime");
    	String end = params.getFirst("endtime");
    	final Date fromDate, toDate;
    	try {
    		fromDate = DATE_FORMAT.parse(start);
    		toDate = DATE_FORMAT.parse(end);
    	} catch (ParseException e) {
    		throw new BadRequestException("Date format of start or end time is invalid.");
    	}
    	
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException {
    	    	Connection con = null;
    	    	try{
    	    		con = RegisterDeviceResource.getDataSource().getConnection();
    	    		Statement statement = con.createStatement();
//    				String query = "SELECT id"
//    						+ " FROM uploads WHERE device = " + deviceId
//    						+ " AND system BETWEEN " + fromDate.getTime() + " AND " + toDate.getTime();
//    	    		ResultSet results = statement.executeQuery(query);
//    	    		while (results.next()) {
//    	    			int upload = results.getInt(1);
//    	    			getData(String.valueOf(deviceId), String.valueOf(upload));
//    	    		}
//    	    		results.close();
    	    		statement.close();
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
	
	private void getData(String device, String upload) {
    	final long systemStartTime = DataStoreResource.getSystemTime(upload);
    	File file = new File(new File(new File(DataStoreResource.DATA_DIR), device), upload);
    	if (!file.exists() || !file.isFile()) {
    		return;
    	}
    	DataVisitor visitor = new DataVisitor() {
			
			@Override
			public void visitTime(long timestamp, long systemTime) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void visitSensor(long timestamp, int sensorType, float x, float y,
					float z) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void visitLocation(long timestamp, int provider, double latitude,
					double longitude, double altitude, float accuracy) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void visitEndOfFile() {
				// TODO Auto-generated method stub
				
			}
		};

	}
    
}
