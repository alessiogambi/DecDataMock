package com.tftpsuite.message;

public class ACKMessage extends Message {
	private short _blockNum;
	
	public ACKMessage(short blockNum, int sourceTID, int destinationTID) {
	   super(MessageType.ACK, sourceTID, destinationTID);
		_blockNum = blockNum;
	}
	
	public short getBlockNum() {
		return _blockNum;
	}

	@Override
   public String toString() {
      String str = super.toString();
      str += " _blockNum: " + _blockNum;
      return str;
   }
   
}
