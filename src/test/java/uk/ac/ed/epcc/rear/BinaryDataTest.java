package uk.ac.ed.epcc.rear;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Random;

import org.junit.Test;


public class BinaryDataTest 
{
	
	@Test
	public void testWriteBinary() throws Exception {
		Random random = new Random();
		DataOutputStream outputStream = new DataOutputStream(new FileOutputStream("/tmp/datatest"));
		for (int i=0; i<10; i++) {
			outputStream.writeByte(1); // version
			outputStream.writeByte(1); // accelerometer
			outputStream.writeLong(new Date().getTime());
			outputStream.writeFloat(random.nextFloat());
			outputStream.writeFloat(random.nextFloat());
			outputStream.writeFloat(random.nextFloat());
		}
		outputStream.close();
	}

}
