package com.tftpsuite.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import com.tftpsuite.message.ERRORMessage;
import com.tftpsuite.message.ErrorType;
import com.tftpsuite.message.Message;
import com.tftpsuite.message.MessageType;

public class ERRORPacket extends TFTPPacket {
   private static final int ERROR_CODE_OFFSET = 2;
   private static final int ERROR_CODE_LENGTH = 2;
   private static final int ERROR_MESSAGE_OFFSET = 4;

   private ERRORMessage _em;
   
   public ERRORPacket(ERRORMessage em, InetAddress _address) {
      super(em, _address);
      _em = em;
   }

   public ERRORMessage getERRORMessage() {
      return _em;
   }
   
   @Override
   protected byte[] getBytes() {
      ByteArrayOutputStream bs = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(bs);
      try {
         ds.writeShort(Message.getMessageTypeCode(MessageType.ERROR));
         ds.writeShort(ERRORMessage.getErrorTypeCode(_em.getErrorType()));
         ds.flush();
         bs.write(_em.getErrorMessage().getBytes("UTF8"));
         bs.write(0);
         bs.flush();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return bs.toByteArray();
   }

   public static ERRORMessage extractERRORMessage(DatagramPacket recvPacket, int destinationTID) {
      byte[] packetData = recvPacket.getData();
      int lenReceived = recvPacket.getLength();
      short errorCode = parseErrorCode(packetData);
      ErrorType errorType = ERRORMessage.getErrorType(errorCode);
      String errorMessage = parseErrorMessage(packetData, lenReceived);
      int sourceTID = recvPacket.getPort();
      return new ERRORMessage(errorType, errorMessage, sourceTID, destinationTID);
   }

   private static String parseErrorMessage(byte[] packetData, int lenReceived) {
      int messageSize = lenReceived - ERROR_MESSAGE_OFFSET;
      if (lenReceived < ERROR_MESSAGE_OFFSET) {
         return null;
      }
      else {
         byte[] data = new byte[messageSize];
         ByteArrayInputStream dataStream = new ByteArrayInputStream(packetData, ERROR_MESSAGE_OFFSET, messageSize);
         dataStream.read(data, 0, messageSize);
         try {
            return new String(data, "UTF8");
         }
         catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
         }
      }
   }

   private static short parseErrorCode(byte[] packetData) {
      ByteArrayInputStream errorCodeStream =
            new ByteArrayInputStream(packetData, ERROR_CODE_OFFSET, ERROR_CODE_LENGTH);
      DataInputStream dis = new DataInputStream(errorCodeStream);
      short errorCode = -1;
      try {
         errorCode = dis.readShort();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return errorCode;
   }

}
