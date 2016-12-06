package uk.ac.ed.epcc;

public class UnknownDeviceException extends Exception 
{
	
	public UnknownDeviceException(String device) {
		super("Unknown device: " + device);
	}

}
