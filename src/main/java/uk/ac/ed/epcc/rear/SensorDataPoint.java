package uk.ac.ed.epcc.rear;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * CREATE TABLE Sensor (upload INT NOT NULL, type INT, timestamp BIGINT, x REAL, y REAL, z REAL, FOREIGN KEY (upload) REFERENCES uploads(id));
 * 
 */
public class SensorDataPoint extends DataPoint {
	
	public static final String TABLE_NAME = "Sensor";
	private final float x;
	private final float y;
	private final float z;

	public SensorDataPoint(int upload, int sensorType, 
			long timestamp, float x, float y, float z) 
	{
		super(upload, sensorType, timestamp);
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public float getZ() {
		return z;
	}
	
	public static String getStatement() {
		return "INSERT INTO " + TABLE_NAME + " (upload, type, timestamp, x, y, z) VALUES (?, ?, ?, ?, ?, ?)";
	}
	
	public void prepareStatement(PreparedStatement statement) throws SQLException {
		statement.setInt(1, upload);
		statement.setInt(2, sensorType);
		statement.setLong(3, timestamp);
        statement.setFloat(4, x);
        statement.setFloat(5, y);
        statement.setFloat(6, z);
	}
	
	@Override
	public String toString() 
	{
		return "DataPoint(Sensor=" + sensorType + ", timestamp=" + timestamp + ", data=(" + x + ", " + y + ", " + z + ")";
	}
	
    public static String toCSV(ResultSet results) 
    {
    	try {
			String sensorType = "";
			switch (results.getInt(2)) {
			case DataPoint.SENSOR_TYPE_ACCELEROMETER:
				sensorType = "A";
				break;
			case DataPoint.SENSOR_TYPE_GYROSCOPE:
				sensorType = "G";
				break;
			case DataPoint.SENSOR_TYPE_MAGNETIC_FIELD:
				sensorType = "M";
				break;
			}
			return String.format("%d,%s,%d,%f,%f,%f\n", 
					results.getInt(1), 
					sensorType, 
					results.getLong(3), 
					results.getFloat(4), 
					results.getFloat(5), 
					results.getFloat(6));
    	} catch (SQLException e) {
    		// failed to convert
    		return "";
    	}
    }


}
