package uk.ac.ed.epcc;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.rmi.ServerException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
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

import org.glassfish.jersey.media.multipart.FormDataParam;

import uk.ac.ed.epcc.rear.DataPoint;

/**
 * CREATE TABLE devices (uuid BINARY(16), timestamp BIGINT, id INT NOT NULL AUTO_INCREMENT PRIMARY KEY);
 * CREATE TABLE uploads (timestamp BIGINT, device INT NOT NULL, id SERIAL PRIMARY KEY, FOREIGN KEY (device) REFERENCES rear.devices(id), start BIGINT, end BIGINT, length BIGINT, system BIGINT, elapsed BIGINT, records BIGINT);
 * ALTER TABLE uploads ADD INDEX `device` (`device`);
 * ALTER TABLE devices ADD INDEX `uuid` (`uuid`);
 * 
 * SELECT(uuid), FROM_UNIXTIME(timestamp/1000), id FROM devices;
 */

@Path("gcrf-REAR")
public class DataStoreResource {

	public static final int VERSION = 1;
	
	private static final String DATA_DIR_PROP = "data.dir";
	private static final String DATA_DIR;
	
	private static final DateFormat DATE_FORMAT  = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss.SSS");
	private static final DateFormat URL_DATE_FORMAT  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
	private static final String[] SENSOR_TYPES = {"A", "G", "M"};

	
	static {
		Properties properties = new Properties();
		try {
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/default.properties");
			if (is != null) properties.load(is);
			else properties.setProperty(DATA_DIR_PROP, "/tmp/data");
		} catch (IOException e) {
			e.printStackTrace();
		}
		DATA_DIR = properties.getProperty(DATA_DIR_PROP);
	}
	
    @Path("/metadata/{device}/{upload}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void receiveMetadata(
    		@PathParam("device") String device, 
    		@PathParam("upload") String upload, 
    		InputStream is) 
    throws SQLException, NamingException, IOException 
    {
    	Connection con = null;
		int deviceId = RegisterDeviceResource.getDevice(device);
		try {
	    	con = getDataSource().getConnection();
			Statement query = con.createStatement();
			int uploadId = -1;
			String s = "SELECT id FROM uploads WHERE id=" + upload + " AND device=" + deviceId;
			ResultSet result = query.executeQuery(s);
//			System.out.println(s);
			if (result.next()) {
				uploadId = result.getInt(1);
			}
			else {
				System.out.println("Not found: " + device + "/" + upload);
        		throw new NotFoundException("Unknown device and upload: " + device + "/" + upload);
			}
			result.close();
			query.close();
			
			DataInputStream ds = new DataInputStream(is);
			byte version = ds.readByte();
    		if (version != VERSION) {
    			new BadRequestException("Unsupported message version: " + version);
    		}
			int numRecords = ds.readInt();
			long systemTime = ds.readLong();
			long elapsedTime = ds.readLong();
			long startTime = ds.readLong();
			long endTime = ds.readLong();
			updateUpload(con, uploadId, startTime, endTime, systemTime, elapsedTime, numRecords);
		}
		catch (EOFException e) {
			// unexpected end of file - invalid format
			throw new BadRequestException("Invalid message format.");
		}
		finally {
			try {
				if (con != null) con.close();
			}
			catch (SQLException e) {
				// ignore
			}
		}
    }

    @Path("/metadata/{device}/{year}/{month}/{day}/{hour}")
    @GET
    @Produces("application/json")
    public Response serveMetadata(
    		@PathParam("device") String device,
    		@PathParam("year") String year,
    		@PathParam("month") String month,
    		@PathParam("day") String day,
    		@PathParam("hour") String hour) 
    {
    	return getMetadata(device, year, month, day, hour);
    }
    
    @Path("/metadata/{device}/{year}/{month}/{day}")
    @GET
    @Produces("application/json")
    public Response serveMetadata(
    		@PathParam("device") String device,
    		@PathParam("year") String year,
    		@PathParam("month") String month,
    		@PathParam("day") String day) 
    {
    	return getMetadata(device, year, month, day, null);
    }

    @Path("/metadata/{device}/{year}/{month}")
    @GET
    @Produces("application/json")
    public Response serveMetadata(
    		@PathParam("device") String device,
    		@PathParam("year") String year,
    		@PathParam("month") String month) 
    {
    	return getMetadata(device, year, month, null, null);
    }

    @Path("/metadata/{device}/{year}")
    @GET
    @Produces("application/json")
    public Response serveMetadata(
    		@PathParam("device") String device,
    		@PathParam("year") String year) 
    {
    	return getMetadata(device, year, null, null, null);
    }

    @Path("/metadata/{device}")
    @GET
    @Produces("application/json")
    public Response serveMetadata(
    		@PathParam("device") String device) 
    {
    	return getMetadata(device, null, null, null, null);
    }

    public Response getMetadata(String device, String year, String month, String day, String hour) {
//    	System.out.println("DEVICE: " + device + ", DATE: " + year + month + day + hour);
    	int deviceId = RegisterDeviceResource.getDevice(device);
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException 
    	    {
    	    	Calendar start = null;
    	    	Calendar end = null;
        		try {
	    	    	if (year != null) {
	    	    		start = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
	    	    		start.setTimeInMillis(0);
	    	    		start.set(Calendar.YEAR, Integer.parseInt(year));
	    	        	end = (Calendar)start.clone();
	    	        	if (month != null) {
	    	        		int m = Integer.parseInt(month)-1;
	    	        		if (m < 0 || m > 11) {
	    	        			throw new NotFoundException();
	    	        		}
	    	        		start.set(Calendar.MONTH, m);
	    	        		end.set(Calendar.MONTH, m);
	    	            	if (day != null) {
	    	            		int d = Integer.parseInt(day);
	    	            		start.set(Calendar.DAY_OF_MONTH, d);
	        	        		end.set(Calendar.DAY_OF_MONTH, d);
	    	                	if (hour != null) {
	    	                		start.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
	    	    	        		end.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY));
	    	                		end.add(Calendar.HOUR_OF_DAY, 1);
	    	                	}
	    	                	else {
	    	                		end.add(Calendar.DAY_OF_MONTH, 1);
	    	                	}
	    	            	}
	    	            	else {
	    	            		end.add(Calendar.MONTH, 1);
	    	            	}
	    	        	}
	    	        	else {
	        	        	end.add(Calendar.YEAR, 1);
	    	        	}
	    	    	}
        		}
        		catch (NumberFormatException e) {
        			throw new NotFoundException();
        		}

    	    	Connection con = null;
    	    	try {
    	    		con = getDataSource().getConnection();
    	    		String query = "SELECT * FROM uploads WHERE device=" + deviceId;
    	    		if (start != null) {
    	    			query += " AND system BETWEEN " + start.getTimeInMillis() + " AND " + end.getTimeInMillis();
    	    		}
	    			// timestamp | device | id | start | end | length | system | elapsed | records
    	    		Statement st = con.createStatement();
    	    		ResultSet result = st.executeQuery(query);
    	    		Writer writer = new BufferedWriter(new OutputStreamWriter(os));
    	    		writer.write("[\n");
    	    		boolean first = true;
    	    		while (result.next()) {
    	    			long system = result.getLong(7);
    	    			if (!result.wasNull()) {
	    	    			if (!first) {
	    	    				writer.write(",\n");
	    	    			}
	    	    			else {
	    	    				first = false;
	    	    			}
	    	    			writer.write(String.format("[%d,%d,%d,%d,%d,%d,%d]",
	    	    					result.getLong(3),
	    	    					result.getLong(4),
	    	    					result.getLong(5),
	    	    					result.getLong(6),
	    	    					system,
	    	    					result.getLong(8),
	    	    					result.getLong(9)));
    	    			}
    	    		}
    	    		writer.write("\n]");
    	    		writer.close();
    	    	}
    	    	catch (SQLException e) {
    	    		throw new ServerException("Could not read uploads", e);
    	    	} catch (NamingException e) {
    	    		throw new ServerException("Could not read uploads", e);
				}
    	    	finally {
    	    		try {
    	    			if (con != null) con.close();
    	    		} catch (SQLException e) {
    	    			// ignore
    	    		}
    	    	}
    	    }
    	};
    	return Response.ok(stream).build();
    }
    
    @Path("/metadata/{device}/date/{from}/{to}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String serveMetadataBetween(
    		@PathParam("device") String device,
    		@PathParam("from") String from,
    		@PathParam("to") String to) 
    {
    	int deviceId = RegisterDeviceResource.getDevice(device);
    	System.out.println("LIST BY DATE - DEVICE: " + deviceId);
		List<Integer> uploads = getUploadsBetween(deviceId, from, to);
    	return uploads.toString();
    }
    
    private List<Integer> getUploadsBetween(int deviceId, String from, String to) {
		Connection con = null;
    	try {
    		Date fromDate = URL_DATE_FORMAT.parse(from);
    		Date toDate = URL_DATE_FORMAT.parse(to);
    		System.out.println("FROM: " + fromDate + ", TO: " + toDate);
			con = getDataSource().getConnection();
			Statement statement = con.createStatement();
			String query = "SELECT id"
					+ " FROM uploads WHERE device = " + deviceId
					+ " AND system BETWEEN " + fromDate.getTime() + " AND " + toDate.getTime();
			System.out.println(query);
			ResultSet rs = statement.executeQuery(query);
			List<Integer> result = new LinkedList<Integer>();
			while (rs.next()) {
				result.add(rs.getInt(1));
			}
			return result;
		} catch (ParseException e) {
			throw new BadRequestException();
		} catch (SQLException e) {
			throw new ServerErrorException(500, e);
		} catch (NamingException e) {
			throw new ServerErrorException(500, e);
		}
    	finally {
    		if (con != null)
				try {
					con.close();
				} catch (SQLException e) {
					// ignore
				}
    	}

    }

    @Path("/data/{device}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public int receiveData(
    		@PathParam("device") String device, 
    		InputStream is) 
    {
    	return receiveData(device, null, null, null, null, is);
    }
    
    @Path("/data/{device}")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public int receiveData(
    		@PathParam("device") String device,
    		@FormDataParam("start") Long start,
    		@FormDataParam("end") Long end,
    		@FormDataParam("system") Long systemTime,
    		@FormDataParam("records") Long numRecords,
    		@FormDataParam("file") InputStream is) 
    {
    	Connection con = null;
    	try {
    		int deviceId = RegisterDeviceResource.getDevice(device);
			long uploadTs = System.currentTimeMillis()/1000;
    		con = getDataSource().getConnection();
			int uploadID = createUpload(con, uploadTs, deviceId, start, end, systemTime, numRecords);
			System.out.println(new Date() + ": Created upload " + uploadID + " for device " + device);
			byte[] buf = new byte[4096];
			OutputStream os = null;
			try {
				File file = getFile(deviceId, uploadID);
				os = new FileOutputStream(file);
				int len;
				while ((len=is.read(buf)) != -1) {
					os.write(buf, 0, len);
				}
				System.out.println(new Date() + ": Upload " + uploadID + " to " + file + " complete for device " + device);
				return uploadID;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new ServerErrorException(500);
			} catch (IOException e) {
				e.printStackTrace();
				throw new ServerErrorException(500);
			} finally {
				try {
					if (os != null)	os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
    	}
    	catch (SQLException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		} catch (NamingException e) {
			e.printStackTrace();
			throw new ServerErrorException(500);
		}
    	finally {
			try {
	    		if (con != null) con.close();
			} catch (SQLException e) {
				// ignore this 
			}
    	}
    	
    }
    
    @Path("/data/{device}/{upload}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response serveData(
    		@PathParam("device") String device,
    		@PathParam("upload") String upload) 
    {
    	int deviceId = RegisterDeviceResource.getDevice(device);
//    	System.out.println("GETTING DATA : " + deviceId + "/" + upload);
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException 
    	    {
    	    	final long systemStartTime = getSystemTime(upload);
    	    	File file = new File(new File(new File(DATA_DIR), String.valueOf(deviceId)), upload);
    	    	if (!file.exists() || !file.isFile()) {
    	    		return;
    	    	}
	    		Writer writer = new BufferedWriter(new OutputStreamWriter(os));
    			writer.write("year,month,day,hour,minute,second,sensortype,x,y,z\n");
    	    	DataInputStream dataStream = new DataInputStream(new FileInputStream(file));
    	    	writeDataFile(dataStream, writer, systemStartTime);
    	    }
    	};
    	return Response.ok(stream).build();
    }
    
    @Path("/data/{device}/date/{from}/{to}")
    @GET
    @Produces("text/csv")
    public Response serveDataBetween(
    		@PathParam("device") String device,
    		@PathParam("from") String from,
    		@PathParam("to") String to) 
    {
    	int deviceId = RegisterDeviceResource.getDevice(device);
//    	System.out.println("DOWNLOAD BY DATE - DEVICE: " + deviceId);
		final List<Integer> uploads = getUploadsBetween(deviceId, from, to);
    	StreamingOutput stream = new StreamingOutput() {
    	    @Override
    	    public void write(OutputStream os) throws IOException, WebApplicationException 
    	    {
	    		Writer writer = new BufferedWriter(new OutputStreamWriter(os));
    			writer.write("year,month,day,hour,minute,second,sensortype,x,y,z\n");
    			try {
	    	    	for (int uploadId : uploads) {
	    	    		String upload = String.valueOf(uploadId);
		    	    	final long systemStartTime = getSystemTime(upload);
		    	    	File file = new File(new File(new File(DATA_DIR), String.valueOf(deviceId)), upload);
		    	    	if (!file.exists() || !file.isFile()) {
		    	    		System.out.println("File " + file + " does not exist or is not a file");
		    	    		continue;
		    	    	}
		    	    	DataInputStream dataStream = new DataInputStream(new FileInputStream(file));
		    	    	int count = writeDataFile(dataStream, writer, systemStartTime);
//		    	    	System.out.println("Wrote " + count + " records from " + file);
		    	    	dataStream.close();
	    	    	}
    			} finally {
    				writer.flush();
    				writer.close();
    			}
    	    }
    	};
    	return Response.ok(stream).build();

    }
    

    
    private int writeDataFile(DataInputStream dataStream, Writer writer, long systemStartTime) throws IOException {
		int count = 0;
		try {
			while (true) {
				if (VERSION != dataStream.readByte()) {
					throw new ServerErrorException(500);
				}
	            int sensorType = dataStream.readByte();
	            long timestamp = dataStream.readLong();
	            switch (sensorType) {
	            case DataPoint.SENSOR_TYPE_ACCELEROMETER:
	            case DataPoint.SENSOR_TYPE_GYROSCOPE:
	            case DataPoint.SENSOR_TYPE_MAGNETIC_FIELD: {
	            	Date trd = new Date(timestamp/1000000 + systemStartTime); // milliseconds
	    			String line = String.format("%s,%s,%.10f,%.10f,%.10f\n",
	    					DATE_FORMAT.format(trd), // absolute timestamp in system time
	    					SENSOR_TYPES[sensorType-1], // sensor type
	    					dataStream.readFloat(),  // X
	    					dataStream.readFloat(),  // Y
	    					dataStream.readFloat()); // Z
	    			writer.write(line);
	            	break;
	            }
	            case DataPoint.TYPE_LOCATION: {
	            	double latitude = dataStream.readDouble();
	            	double longitude = dataStream.readDouble();
	            	double altitude = dataStream.readDouble();
	            	float accuracy = dataStream.readFloat();
	            	break;
	            }
	            case DataPoint.TYPE_TIME: {
	            	long systemTime = dataStream.readLong();
	            	break;
	            }
	            default:
	            	// unsupported sensor type
	            	break;
	            }
	            count++;
			}
		}
		catch (EOFException e) {
		}
		finally {
			writer.flush();
		}
		return count;
    }
    
    private static int updateUpload(Connection con, long uploadId, 
    		long startTime, long endTime, 
    		long systemTime, long elapsedTime, 
    		int numRecords)
    		throws SQLException 
    {
    	Statement statement = null;
    	try {
    		statement = con.createStatement();
	    	String update = "UPDATE uploads SET start=" + startTime + ", end=" + endTime 
	    			+ ", length=" + (endTime-startTime)
	    			+ ", system=" + systemTime
	    			+ ", elapsed=" + elapsedTime
	    			+ ", records=" + numRecords 
	    			+ " WHERE id=" + uploadId;
	    	System.out.println("Updating upload metadata: " + update);
	    	return statement.executeUpdate(update);
    	} finally {
    		if (statement != null) statement.close();
    	}
    }
	
	private static int createUpload(Connection con, long uploadTs, int deviceId, 
			Long startTime, Long endTime, Long systemTime, Long numRecords)
			throws SQLException {
		Statement s = null;
		ResultSet result = null;
		try {
			s = con.createStatement();
			StringBuilder columns = new StringBuilder();
			StringBuilder values = new StringBuilder();
			columns.append("(timestamp, device");
			values.append("(").append(uploadTs);
			values.append(",").append(deviceId);
			if (startTime != null) {
				columns.append(", start");
				values.append(",").append(startTime);
			}
			if (endTime != null) {
				columns.append(", end");
				values.append(",").append(endTime);
			}
			if (startTime != null && endTime != null) {
				long length = endTime-startTime;
				columns.append(", length");
				values.append(",").append(length);
			}
			if (systemTime != null) {
				columns.append(", system");
				values.append(",").append(systemTime);
			}
			if (numRecords != null) {
				columns.append(", records");
				values.append(",").append(numRecords);
			}
			columns.append(")");
			values.append(")");
			String query = "INSERT INTO uploads " + columns + " VALUES " + values;
	//		System.out.println("New upload: " + query);
			s.executeUpdate(query);
			result = s.executeQuery("SELECT MAX(id) FROM uploads WHERE timestamp=" + uploadTs);
			if (result.next()) {
				return result.getInt(1);
			}
			else {
				throw new ServerErrorException(500);
			}
		} finally {
			if (result != null) result.close();
			if (s != null) s.close();
		}
    }
	
	private static long getSystemTime(String upload) {
		String query = "SELECT start, elapsed, system FROM uploads WHERE id=" + upload;
		Connection con = null;
		Statement s = null;
		ResultSet results = null;
		try {
			con = getDataSource().getConnection();
			s = con.createStatement();
			results = s.executeQuery(query);
			if (results.next()) {
				// timestamp  | device | id | start | end | length | system | elapsed | records
				long start = results.getLong(1);
				long elapsed = results.getLong(2);
				long system = results.getLong(3);
				return system + (elapsed - start/1000000); // start in millis relative to system time
			}
			throw new NotFoundException();
		} catch (SQLException e) {
			throw new ServerErrorException(500);
		} catch (NamingException e1) {
			throw new ServerErrorException(500);
		} finally {
			try {
				if (results != null) results.close();
			} catch (SQLException e) {
				// ignore
			}
			try {
				if (s != null) s.close();
			} catch (SQLException e) {
				// ignore
			}
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				// ignore
			}
		}
	}

	public static DataSource getDataSource() throws NamingException
	{
		InitialContext cxt = new InitialContext();
		DataSource ds = (DataSource) cxt.lookup( "java:/comp/env/jdbc/rear_meta_db" );
		return ds;
	}
	 
    private static File getFile(int device, int upload) {
    	File dir = new File(DATA_DIR, String.valueOf(device));
    	if (!dir.exists()) dir.mkdirs();
    	return new File(dir, String.valueOf(upload));
	}
	
}
