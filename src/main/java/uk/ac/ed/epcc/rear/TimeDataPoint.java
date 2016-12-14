package uk.ac.ed.epcc.rear;


/**
 * 
 * CREATE TABLE Time (upload BIGINT UNSIGNED NOT NULL, timestamp BIGINT, systemTime BIGINT, FOREIGN KEY (upload) REFERENCES uploads(id));
 *
 */
public class TimeDataPoint extends DataPoint {

	public static final String TABLE_NAME = "Time";
	private long systemTime;

	public TimeDataPoint(int upload, long timestamp, long systemTime) {
		super(upload, DataPoint.TYPE_TIME, timestamp);
		this.systemTime = systemTime;
	}
	
	public long getSystemTime() {
		return systemTime;
	}
	
	public String getStatement() {
		return "INSERT INTO " + TABLE_NAME 
				+ " (upload, timestamp, systemTime) VALUES ("
				+ upload + "," + timestamp + "," + systemTime + ")";
	}

}
