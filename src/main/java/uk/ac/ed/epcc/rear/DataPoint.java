package uk.ac.ed.epcc.rear;

import java.io.DataInputStream;
import java.io.IOException;

import javax.ws.rs.ServerErrorException;

public abstract class DataPoint
{
	
	public static final int SENSOR_TYPE_ACCELEROMETER = 1;
	public static final int SENSOR_TYPE_GYROSCOPE = 2;
	public static final int SENSOR_TYPE_MAGNETIC_FIELD = 3;
	public static final int TYPE_LOCATION = 4;
	public static final int TYPE_TIME = 5;
	
	protected final int upload;
	protected final int sensorType;
	protected final long timestamp;
	
	public DataPoint(int upload, int sensorType, long timestamp) {
		this.upload = upload;
		this.sensorType = sensorType;
		this.timestamp = timestamp;
	}
	
	public int getUploadId() {
		return upload;
	}
	
	public int getSensorType() {
		return sensorType;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	public static DataPoint read(DataInputStream dataStream, int supported, int uploadID) throws IOException 
	{
		byte version = dataStream.readByte();
		if (version != supported) {
			new ServerErrorException("Unsupported message version: " + version, 400);
		}
        int sensorType = dataStream.readByte();
        long timestamp = dataStream.readLong();
        DataPoint dataPoint = null;
        switch (sensorType) {
		    case DataPoint.SENSOR_TYPE_ACCELEROMETER:
		    case DataPoint.SENSOR_TYPE_GYROSCOPE:
		    case DataPoint.SENSOR_TYPE_MAGNETIC_FIELD: {
		        float x = dataStream.readFloat();
		        float y = dataStream.readFloat();
		        float z = dataStream.readFloat();
		        dataPoint = new SensorDataPoint(uploadID, sensorType, timestamp, x, y, z);
		    	break;
		    }
		    case DataPoint.TYPE_LOCATION: {
		    	double latitude = dataStream.readDouble();
		    	double longitude = dataStream.readDouble();
		    	double altitude = dataStream.readDouble();
		    	float accuracy = dataStream.readFloat();
		    	dataPoint =	new LocationDataPoint(uploadID, sensorType, timestamp, 
		    					latitude, longitude, altitude, accuracy);
			}
        }
        return dataPoint;
	}

}
