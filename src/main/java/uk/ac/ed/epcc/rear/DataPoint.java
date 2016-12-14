package uk.ac.ed.epcc.rear;

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


}
