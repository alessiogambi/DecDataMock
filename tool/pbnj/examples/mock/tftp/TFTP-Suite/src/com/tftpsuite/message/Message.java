package com.tftpsuite.message;

public abstract class Message {
	
   public static short getMessageTypeCode(MessageType mt) {
      switch (mt) {
         case ACK:
            return 4;
            
         case DATA:
            return 3;
            
         case ERROR:
            return 5;
            
         case RRQ:
            return 1;
            
         case WRQ:
            return 2;
            
         default:
            return -1;
      }
   }
   
   public static MessageType getMessageTypeFromCode(short messageCode) {
      switch (messageCode) {
         case 1:
            return MessageType.RRQ;
            
         case 2:
            return MessageType.WRQ;
            
         case 3:
            return MessageType.DATA;
            
         case 4:
            return MessageType.ACK;
            
         case 5:
            return MessageType.ERROR;
            
         default:
               return null;         
      }
   }
   
   protected MessageType _messageType;
	private int _sourceTID;
	private int _destinationTID;
	
	public Message(MessageType messageType, int sourceTID, int destinationTID) {
	   _messageType = messageType;
	   _sourceTID = sourceTID;
	   _destinationTID = destinationTID;
	}
	
	public final MessageType getMessageType() {
		return _messageType;
	}
	
	public final int getSourceTID() {
	   return _sourceTID;
	}
	
	public final int getDestinationTID() {
	   return _destinationTID;
	}
	
	@Override
	public String toString() {
      return   "Message: " + super.toString() +
      " _messageType: " + (_messageType == null ? "null" : _messageType.toString()) +
      " _sourceTID: " + _sourceTID +
      " _destinationTID: " + _destinationTID;
	}
	
}

