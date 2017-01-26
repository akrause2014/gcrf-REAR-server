package uk.ac.ed.epcc;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;

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
	
    @Path("/data/{device}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public int receiveData(
    		@PathParam("device") String device, 
    		InputStream is) 
    {
    	return receiveData(device, null, null, null, null, is);
    }
    
    @Path("/metadata/{device}/{upload}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void receiveData(
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
			System.out.println(s);
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
    			new ServerErrorException("Unsupported message version: " + version, 400);
    		}
			int numRecords = ds.readInt();
			long systemTime = ds.readLong();
			long elapsedTime = ds.readLong();
			long startTime = ds.readLong();
			long endTime = ds.readLong();
			updateUpload(con, uploadId, startTime, endTime, systemTime, elapsedTime, numRecords);
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
    		if (con != null) {
    			try {
					con.close();
				} catch (SQLException e) {
					// ignore this 
				}
    		}
    	}
    	
    }
    
    private static void updateUpload(Connection con, long uploadId, 
    		long startTime, long endTime, 
    		long systemTime, long elapsedTime, 
    		int numRecords)
    		throws SQLException 
    {
    	Statement statement = con.createStatement();
    	String update = "UPDATE uploads SET start=" + startTime + ", end=" + endTime 
    			+ ", length=" + (endTime-startTime)
    			+ ", system=" + systemTime
    			+ ", elapsed=" + elapsedTime
    			+ ", records=" + numRecords 
    			+ " WHERE id=" + uploadId;
    	System.out.println("Updating upload metadata: " + update);
    	int result = statement.executeUpdate(update);
    	statement.close();
    }
	
	private static int createUpload(Connection con, long uploadTs, int deviceId, 
			Long startTime, Long endTime, Long systemTime, Long numRecords)
			throws SQLException {
		Statement s = con.createStatement();
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
		ResultSet result = s.executeQuery("SELECT MAX(id) FROM uploads WHERE timestamp=" + uploadTs);
		if (result.next()) {
			return result.getInt(1);
		}
		else {
			throw new ServerErrorException(500);
		}
    }

	private static DataSource getDataSource() throws NamingException
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
