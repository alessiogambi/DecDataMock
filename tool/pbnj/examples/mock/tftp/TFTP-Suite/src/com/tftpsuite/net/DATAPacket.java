package com.tftpsuite.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import com.tftpsuite.message.DATAMessage;
import com.tftpsuite.message.Message;
import com.tftpsuite.message.MessageType;

public class DATAPacket extends TFTPPacket {
   private static final int BLOCKNUM_OFFSET = 2;
   private static final int BLOCKNUM_LENGTH = 2;
   private static final int DATA_OFFSET = 4;
   
   private DATAMessage _dm;
   
   public DATAPacket(DATAMessage dm, InetAddress address) {
      super(dm, address);
      _dm = dm;
   }

   public DATAMessage getDATAMessage() {
      return _dm;
   }
   
   public static DATAMessage extractDATAMessage(DatagramPacket recvPacket, int destinationTID) {
      byte[] packetData = recvPacket.getData();
      int lenReceived = recvPacket.getLength();
      short blockNum = parseBlockNum(packetData);
      byte[] data = parseData(packetData, lenReceived);
      int sourceTID = recvPacket.getPort();
      return new DATAMessage(data, blockNum, sourceTID, destinationTID);
   }
   
   private static byte[] parseData(byte[] packetData, int lenReceived) {
      int dataSize = lenReceived - DATA_OFFSET;
      if (dataSize > DATAMessage.MAX_DATA_SIZE || lenReceived < DATA_OFFSET) {
         return null;
      }
      else {
         byte[] data = new byte[dataSize];
         ByteArrayInputStream dataStream = new ByteArrayInputStream(packetData, DATA_OFFSET, dataSize);
         dataStream.read(data, 0, dataSize);
         return data;
      }
   }

   private static short parseBlockNum(byte[] packetData) {
      ByteArrayInputStream blockNumStream = new ByteArrayInputStream(packetData, BLOCKNUM_OFFSET, BLOCKNUM_LENGTH);
      DataInputStream dis = new DataInputStream(blockNumStream);
      short blockNum = -1;
      try {
         blockNum = dis.readShort();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return blockNum;
   }

   @Override
   protected byte[] getBytes() {
      ByteArrayOutputStream bs = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(bs);
      try {
         ds.writeShort(Message.getMessageTypeCode(MessageType.DATA));
         ds.writeShort(_dm.getBlockNum());
         ds.flush();
         bs.write(_dm.getData());
         bs.flush();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return bs.toByteArray();
   }
   
}
