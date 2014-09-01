package com.tftpsuite.message;

public class DATAMessage extends Message {
   public static final int MAX_DATA_SIZE = 8;

   private byte[] _data;
	private short _blockNum;
	
	public DATAMessage(byte[] data, short blockNum, int sourceTID, int destinationTID) {
	   super(MessageType.DATA, sourceTID, destinationTID);
		_data = data;
		_blockNum = blockNum;
	}
	
	public byte[] getData() {
		return _data;
	}
	
	public short getBlockNum() {
		return _blockNum;
	}

   @Override
   public String toString() {
      String str = super.toString();
      str += " _blockNum: " + _blockNum;
      str += " _data: ";
      if (_data == null) {
         str += "null";
      }
      else {
         str += "{";
         int i;
         for (i = 0; i < _data.length - 1; i++) {
            str += _data[i] + ", ";
         }
         str += _data[i];        
         str += "}";
      }
      return str;
   }
   
}
