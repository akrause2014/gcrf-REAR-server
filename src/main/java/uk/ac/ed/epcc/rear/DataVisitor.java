package uk.ac.ed.epcc.rear;

public interface DataVisitor {
	
	void visitSensor(long timestamp, int sensorType, float x, float y, float z);
	void visitLocation(long timestamp, int provider, double latitude, double longitude, double altitude, float accuracy);
	void visitTime(long timestamp, long systemTime);
	void visitEndOfFile();

}
