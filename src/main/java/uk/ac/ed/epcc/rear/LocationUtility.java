package uk.ac.ed.epcc.rear;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.ws.rs.ServerErrorException;

public class LocationUtility {
	
	public static final int VERSION = 1;
	
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			throw new IllegalArgumentException("Missing arguments: input and output file names");
		}
		DataInputStream dataStream = new DataInputStream(new FileInputStream(args[0]));
		Writer writer = new FileWriter(args[1]);
		int count = 0;
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
	            	break;
	            }
	            case DataPoint.TYPE_LOCATION: {
	            	double latitude = dataStream.readDouble();
	            	double longitude = dataStream.readDouble();
	            	double altitude = dataStream.readDouble();
	            	float accuracy = dataStream.readFloat();
	            	writer.write(String.format("%f,%f,%f\n", latitude, longitude, altitude));
	            	count++;
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
			}
			
		} catch (EOFException e) {
			// end of file
			writer.flush();
			writer.close();
		}
		System.out.println("Wrote " + count + " location records to " + args[1] + ".\n");
	}

}
