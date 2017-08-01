package uk.ac.ed.epcc.rear;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.ws.rs.ServerErrorException;

import uk.ac.ed.epcc.DataStoreResource;

public class DataReader {
	
	public void read(File file, DataVisitor visitor) throws IOException 
	{
		DataInputStream dataStream = null;
		try {
			dataStream = new DataInputStream(new FileInputStream(file));
			while (true) {
	    		byte version = dataStream.readByte();
	    		if (version != DataStoreResource.VERSION) {
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
	                visitor.visitSensor(timestamp, sensorType, x, y, z);
	            	break;
	            }
	            case DataPoint.TYPE_LOCATION:
	            case DataPoint.TYPE_LOCATION_NETWORK: {
	            	double latitude = dataStream.readDouble();
	            	double longitude = dataStream.readDouble();
	            	double altitude = dataStream.readDouble();
	            	float accuracy = dataStream.readFloat();
	            	visitor.visitLocation(timestamp, sensorType, latitude, longitude, altitude, accuracy);
	            	break;
	            }
	            case DataPoint.TYPE_TIME: {
	            	long systemTime = dataStream.readLong();
	            	visitor.visitTime(timestamp, systemTime);
	            	break;
	            }
	            default:
	            	// unsupported sensor type
	            	break;
	            }
			}
			
		} catch (EOFException e) {
			// end of file
			visitor.visitEndOfFile();
		}
		
		if (dataStream != null) dataStream.close();

    }

}
