package uk.ac.ed.epcc.rear;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 
 * CREATE TABLE Location (upload BIGINT UNSIGNED NOT NULL, timestamp BIGINT, latitude REAL, longitude REAL, altitude REAL, accuracy REAL, FOREIGN KEY (upload) REFERENCES uploads(id));
 *
 */
public class LocationDataPoint extends DataPoint {

	private static final String TABLE_NAME = "Location";

	private double latitude;
	private double longitude;
	private double altitude;
	private float accuracy;

	public LocationDataPoint(int upload, int sensorType, long timestamp, 
			double latitude, double longitude, double altitude, float accuracy) {
		super(upload, sensorType, timestamp);
		
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.accuracy = accuracy;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public double getAltitude() {
		return altitude;
	}
	
	public static String getStatement() {
		return "INSERT INTO " + TABLE_NAME 
				+ " (upload, timestamp, latitude, longitude, altitude, accuracy)"
				+ " VALUES (?, ?, ?, ?, ?, ?)";
	}
	
	public void prepareStatement(PreparedStatement statement) throws SQLException {
		statement.setInt(1, upload);
		statement.setLong(2, timestamp);
        statement.setDouble(3, latitude);
        statement.setDouble(4, longitude);
        statement.setDouble(5, altitude);
        statement.setFloat(6, accuracy);
	}


}
