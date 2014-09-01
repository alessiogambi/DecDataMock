package com.tftpsuite.message;

public class WRQMessage extends Message {

   private String _filename;
   private String _mode;

   public WRQMessage(String filename, String mode, int sourceTID, int destinationTID) {
      super(MessageType.WRQ, sourceTID, destinationTID);
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
