package uk.ac.ed.epcc.rear;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;

import javax.ws.rs.ServerErrorException;

public class MetadataUtility {
	
	public static final int VERSION = 1;
	
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			throw new IllegalArgumentException("Missing arguments: file name");
		}
		DataInputStream dataStream = new DataInputStream(new FileInputStream(args[0]));
		int count = 0;
		long startElapsed = -1;
		long endElapsed = -1;
		long system = -1;
		long elapsed = -1;
		try {
			while (true) {
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
	                if (startElapsed < 0 || startElapsed > timestamp) {
	                	startElapsed = timestamp;
	                }
	                endElapsed = timestamp;
		            count++;
	            	break;
	            }
	            case DataPoint.TYPE_LOCATION:
	            case DataPoint.TYPE_LOCATION_NETWORK: {
	            	double latitude = dataStream.readDouble();
	            	double longitude = dataStream.readDouble();
	            	double altitude = dataStream.readDouble();
	            	float accuracy = dataStream.readFloat();
	            	System.out.println(
	            			String.format("Location: lat/lon=%f/%f, altitude=%f, accuracy=%.1f", 
	            					latitude, longitude, altitude, accuracy));
	            	break;
	            }
	            case DataPoint.TYPE_TIME: {
	            	long systemTime = dataStream.readLong();
	            	System.out.println("System time: " + systemTime + ", elapsed: " + timestamp);
	            	system = systemTime;
	            	elapsed = timestamp;
	            	break;
	            }
	            default:
	            	// unsupported sensor type
	            	break;
	            }
			}
			
		} catch (EOFException e) {
			// end of file
		}
		System.out.println("Read " + count + " records.\n");
    	System.out.println("System time:   " + system);
    	System.out.println("Elapsed time:  " + elapsed);
    	System.out.println("Start elapsed: " + startElapsed);
    	System.out.println("End elapsed:   " + endElapsed);
    	long length = endElapsed-startElapsed;
    	System.out.println("Length:        " + length);
    	System.out.println("Records:       " + count);
    	System.out.println("\nUPDATE uploads SET start=" + startElapsed + ", end=" + endElapsed 
    			+ ", length=" + length + ", system=" + system + ", elapsed=" + elapsed + ", records=" + count);
	}

}
