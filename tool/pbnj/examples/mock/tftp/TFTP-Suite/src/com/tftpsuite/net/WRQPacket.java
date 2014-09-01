package com.tftpsuite.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import com.tftpsuite.message.Message;
import com.tftpsuite.message.MessageType;
import com.tftpsuite.message.WRQMessage;

public class WRQPacket extends TFTPPacket{
   
   private static final int FILENAME_OFFSET = 2;

   private WRQMessage _wrq;
   
   public WRQPacket(WRQMessage wrq, InetAddress address) {
      super(wrq, address);
      _wrq = wrq;
   }
   
   public WRQMessage getWRQMessage() {
      return _wrq;
   }
   
   @Override
   protected byte[] getBytes() {
      ByteArrayOutputStream bs = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(bs);
      try {
         ds.writeShort(Message.getMessageTypeCode(MessageType.WRQ));
         ds.flush();
         bs.write(_wrq.getFilename().getBytes("UTF8"));
         bs.write(0);
         bs.write(_wrq.getMode().getBytes("UTF8"));
         bs.write(0);
         bs.flush();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return bs.toByteArray();
   }
   
   public static WRQMessage extractWRQMessage(DatagramPacket recvPacket, int destinationTID) {
      byte[] packetData = recvPacket.getData();
      int lenReceived = recvPacket.getLength();
      String filename = parseFilename(packetData, lenReceived);
      int sourceTID = recvPacket.getPort();
      String mode = parseMode(packetData, filename.length() + 1 + FILENAME_OFFSET, lenReceived);
      return new WRQMessage(filename, mode, sourceTID, destinationTID);
   }
   
   private static String parseFilename(byte[] packetData, int lenReceived) {
      int messageSize = lenReceived - FILENAME_OFFSET;
      if (lenReceived < FILENAME_OFFSET) {
         return null;
      }
      else {
         ByteArrayInputStream dataStream = new ByteArrayInputStream(packetData, FILENAME_OFFSET, messageSize);
         ByteArrayOutputStream filenameStream = new ByteArrayOutputStream();
         byte currentByte;
         do {
            currentByte = (byte)(dataStream.read());
            filenameStream.write(currentByte);
         } while (currentByte != 0);
         filenameStream.write(0);                  
         try {
            return new String(filenameStream.toByteArray(), "UTF8");
         }
         catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
         }
      }
   }

   private static String parseMode(byte[] packetData, int modeOffset, int lenReceived) {
      int messageSize = lenReceived - modeOffset;
      if (lenReceived < modeOffset) {
         return null;
      }
      else {
         byte[] data = new byte[messageSize];
         ByteArrayInputStream dataStream = new ByteArrayInputStream(packetData, modeOffset, messageSize);
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

}
