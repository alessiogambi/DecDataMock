package com.tftpsuite.message;

public class RRQMessage extends Message {
	private String _filename;
	private String _mode;
	
	public RRQMessage(String filename, String mode, int sourceTID, int destinationTID) {
	   super(MessageType.RRQ, sourceTID, destinationTID);
		_filename = filename;
		_mode = mode;
	}
	
	public String getFilename() {
		return _filename;
	}
	
	public String getMode() {
		return _mode;
	}

}
