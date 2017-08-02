package uk.ac.ed.epcc.rear;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

import org.junit.Test;

import uk.ac.ed.epcc.LocationResource;

/**
 * 
 *
 */

public class BinaryDataTest 
{
	
	public static final int VERSION = 1;
	
	@Test
	public void testWriteBinary() throws Exception {
		Random random = new Random();
		DataOutputStream outputStream = new DataOutputStream(new FileOutputStream("/tmp/datatest"));
		outputStream.writeByte(VERSION);
		outputStream.writeByte(DataPoint.TYPE_TIME);
		outputStream.writeLong(1000000);
		long currentTime = System.currentTimeMillis();
		outputStream.writeLong(currentTime);
		System.out.println("(" + 1000000 + ", " + currentTime + ")");
		
		outputStream.writeByte(VERSION);
		outputStream.writeByte(DataPoint.TYPE_LOCATION);
		outputStream.writeLong(1000000);
		double lon = 55.921817;
		double lat = -3.173189;
		outputStream.writeDouble(lon);
		outputStream.writeDouble(lat);
		outputStream.writeDouble(47);
		outputStream.writeFloat(2);
		System.out.println("(" + 1000000 + ", " + lon + ", " + lat + ", " + 47d + ", " + 2f + ")");
		
		outputStream.writeByte(VERSION);
		outputStream.writeByte(DataPoint.TYPE_LOCATION_NETWORK);
		outputStream.writeLong(1000000);
		outputStream.writeDouble(lon);
		outputStream.writeDouble(lat);
		outputStream.writeDouble(47);
		outputStream.writeFloat(2);
		System.out.println("(" + 1000000 + ", " + lon + ", " + lat + ", " + 47d + ", " + 2f + ")");

		for (int i=0; i<10; i++) {
			outputStream.writeByte(VERSION); // version
			outputStream.writeByte(1); // sensor type: accelerometer
			int timestamp = 1000000+i*20;
			outputStream.writeLong(timestamp); // timestamp
			float x = random.nextFloat();
			float y = random.nextFloat();
			float z = random.nextFloat();
			outputStream.writeFloat(x); // X
			outputStream.writeFloat(y); // Y
			outputStream.writeFloat(z); // Z
			System.out.println("(" + timestamp + ", " + x + ", " + y + ", " + z + ")");
		}
		outputStream.close();
	}

	@Test
	public void testWriteMetadata() throws Exception {
		for (int i=0; i<20; i++) {
			DataOutputStream outputStream = new DataOutputStream(new FileOutputStream("/tmp/metadatatest" + i));
			outputStream.writeByte(1); // version
			outputStream.writeInt(100); // number of records
			Calendar cal = new GregorianCalendar();
			cal.add(Calendar.DAY_OF_MONTH, -i);
			cal.add(Calendar.HOUR_OF_DAY, -i);
			outputStream.writeLong(cal.getTimeInMillis()); // system timestamp in millis
			outputStream.writeLong(1000l); // elapsed time in millis matching timestamp
			outputStream.writeLong(1000000000l); // start time (nanos)
			outputStream.writeLong(3000000000l); // end time (nanos)
			outputStream.close();
		}
	}
	
	@Test
	public void testWriteLocation() throws Exception {
		double lon = 55.921817;
		double lat = -3.173189;
		DataOutputStream outputStream = new DataOutputStream(new FileOutputStream("/tmp/locationtest"));
		outputStream.writeLong(new Date().getTime());
		outputStream.writeInt(0);
		outputStream.writeDouble(lon);
		outputStream.writeDouble(lat);
		outputStream.writeFloat(10.0f);
		outputStream.writeLong(new Date().getTime());
		outputStream.writeInt(0);
		outputStream.writeDouble(lon);
		outputStream.writeDouble(lat);
		outputStream.writeFloat(10.0f);
		outputStream.close();
	}

}
