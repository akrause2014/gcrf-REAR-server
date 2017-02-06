package uk.ac.ed.epcc.rear;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import org.junit.Test;


public class BinaryDataTest 
{
	
	@Test
	public void testWriteBinary() throws Exception {
		Random random = new Random();
		DataOutputStream outputStream = new DataOutputStream(new FileOutputStream("/tmp/datatest"));
		outputStream.writeByte(1);
		outputStream.writeByte(DataPoint.TYPE_TIME);
		outputStream.writeLong(1000000);
		outputStream.writeLong(System.currentTimeMillis());
		outputStream.writeByte(1);
		outputStream.writeByte(DataPoint.TYPE_LOCATION);
		outputStream.writeLong(1000000);
		outputStream.writeDouble(55.9533);
		outputStream.writeDouble(3.1883);
		outputStream.writeDouble(47);
		outputStream.writeFloat(2);
		for (int i=0; i<10; i++) {
			outputStream.writeByte(1); // version
			outputStream.writeByte(1); // sensor type: accelerometer
			outputStream.writeLong(1000000+i*20000000); // timestamp
			outputStream.writeFloat(random.nextFloat()); // X
			outputStream.writeFloat(random.nextFloat()); // Y
			outputStream.writeFloat(random.nextFloat()); // Z
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

}
