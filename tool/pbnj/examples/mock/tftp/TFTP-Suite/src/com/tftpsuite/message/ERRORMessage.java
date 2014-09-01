package com.tftpsuite.message;

public class ERRORMessage extends Message {

   public static String getErrorTypeMessage(ErrorType et) {
      switch (et) {
         case ACCESS_VIOLATION:
            return "Access violation";
            
         case DISK_FULL_OR_ALLOCATION_EXCEEDED:
            return "Disk full or allocation exceeded";
            
         case FILE_ALREADY_EXISTS:
            return "File already exists";
            
         case FILE_NOT_FOUND:
            return "File not found";
            
         case ILLEGAL_TFTP_OPERATION:
            return "Illegal TFTP operation";
            
         case NO_SUCH_USER:
            return "No such user";
            
         case UNDEFINED:
            return "Undefined error";
            
         case UNKOWN_TRANSFER_ID:
            return "Unknown transfer ID";
            
         default:
            return null;
      }
   }
   
   public static short getErrorTypeCode(ErrorType et) {
      switch (et) {
         case ACCESS_VIOLATION:
            return 2;
            
         case DISK_FULL_OR_ALLOCATION_EXCEEDED:
            return 3;
            
         case FILE_ALREADY_EXISTS:
            return 6;
            
         case FILE_NOT_FOUND:
            return 1;
            
         case ILLEGAL_TFTP_OPERATION:
            return 4;
            
         case NO_SUCH_USER:
            return 7;
            
         case UNDEFINED:
            return 0;
            
         case UNKOWN_TRANSFER_ID:
            return 5;
            
         default:
            return -1;
      }
   }
   
   public static ErrorType getErrorType(short errorCode) {
      switch (errorCode) {
         case 0:
            return ErrorType.UNDEFINED;
            
         case 1:
            return ErrorType.FILE_NOT_FOUND;
            
         case 2:
            return ErrorType.ACCESS_VIOLATION;
            
         case 3:
            return ErrorType.DISK_FULL_OR_ALLOCATION_EXCEEDED;
            
         case 4:
            return ErrorType.ILLEGAL_TFTP_OPERATION;
            
         case 5:
            return ErrorType.UNKOWN_TRANSFER_ID;
            
         case 6:
            return ErrorType.FILE_ALREADY_EXISTS;
            
         case 7:
            return ErrorType.NO_SUCH_USER;
            
         default:
            return null;
      }
   }
   
   private ErrorType _errorType;
   private String _errorMessage;
   
   public ERRORMessage(ErrorType errorType, String errorMessage, int sourceTID, int destinationTID) {
      super(MessageType.ERROR, sourceTID, destinationTID);
      _errorType = errorType;
      _errorMessage = errorMessage;
   }

   public ErrorType getErrorType() {
      return _errorType;
   }
   
   public String getErrorMessage() {
      return _errorMessage;
   }

}
