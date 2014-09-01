package com.tftpsuite.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import com.tftpsuite.message.ACKMessage;
import com.tftpsuite.message.Message;
import com.tftpsuite.message.MessageType;

public class ACKPacket extends TFTPPacket {
   private static final int BLOCKNUM_OFFSET = 2;
   private static final int BLOCKNUM_LENGTH = 2;

   private ACKMessage _am;
   
   public ACKPacket(ACKMessage am, InetAddress address) {
      super(am, address);
      _am = am;
   }

   @Override
   protected byte[] getBytes() {
      ByteArrayOutputStream bs = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(bs);
      try {
         ds.writeShort(Message.getMessageTypeCode(MessageType.ACK));
         ds.writeShort(_am.getBlockNum());
         ds.flush();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return bs.toByteArray();
   }

   public static ACKMessage extractACKMessage(DatagramPacket recvPacket, int destinationTID) {
      byte[] packetData = recvPacket.getData();
      short blockNum = parseBlockNum(packetData);
      int sourceTID = recvPacket.getPort();
      return new ACKMessage(blockNum, sourceTID, destinationTID);
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
   
}
