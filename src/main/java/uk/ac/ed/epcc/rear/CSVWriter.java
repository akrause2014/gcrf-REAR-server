package uk.ac.ed.epcc.rear;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CSVWriter {
	
	public static final String[] SENSOR_TYPES = {"A", "G", "M"};
	public static final DateFormat DATE_FORMAT  = new SimpleDateFormat("yyyy,MM,dd,HH,mm,ss,SSS");
	public static final DateFormat FILENAME_DATE_FORMAT  = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
	
	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		FILENAME_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}


	public static void main(String[] args) throws IOException 
	{
		File file = new File(args[0]);
		DataReader reader = new DataReader();
		System.out.println("year,month,day,hour,minute,second,millisecond,sensortype,x,y,z");
		PrintCSVVisitor visitor = new PrintCSVVisitor();
		reader.read(file, visitor);
	}
	
	public static class PrintCSVVisitor implements DataVisitor {
		
		private long systemStartTime;
		private Date startTimestamp;
		private Date endTimestamp;

		@Override
		public void visitSensor(long timestamp,	int sensorType, 
				float x, float y, float z) 
		{
			if (systemStartTime == 0) {
				System.err.println("No timestamp");
				System.exit(-1);
			}
        	Date trd = new Date(timestamp/1000000 + systemStartTime); // milliseconds
        	if (startTimestamp == null || startTimestamp.after(trd)) {
        		startTimestamp = trd;
        	}
        	endTimestamp = trd;
			String line = String.format("%s,%s,%.10f,%.10f,%.10f",
					DATE_FORMAT.format(trd),
					SENSOR_TYPES[sensorType-1],
					x, y, z);
			System.out.println(line);
		}

		@Override
		public void visitLocation(long timestamp, int provider,
				double latitude, double longitude, double altitude,
				float accuracy) {
		}

		@Override
		public void visitTime(long timestamp, long systemTime) {
			systemStartTime = systemTime-timestamp;
		}

		@Override
		public void visitEndOfFile() {
			System.err.println(
					FILENAME_DATE_FORMAT.format(startTimestamp) + 
					"-" + FILENAME_DATE_FORMAT.format(endTimestamp));
		}
		
	}

}
