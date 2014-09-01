package com.tftpsuite.net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

import com.tftpsuite.message.Message;

public abstract class TFTPPacket {
   private static final int OPCODE_OFFSET = 0;
   private static final int OPCODE_LENGTH = 2;

   private InetAddress _address;
   private int _port;
   
   public TFTPPacket(Message message, InetAddress address) {
      _address = address;
      _port = message.getDestinationTID();      
   }
   
   protected abstract byte[] getBytes();
   
   public DatagramPacket toDatagramPacket() {
      byte[] bytes = getBytes();
      return new DatagramPacket(bytes, bytes.length, _address, _port);
   }

   public static short parseOpcode(byte[] packetData) {
      ByteArrayInputStream opStream = new ByteArrayInputStream(packetData, OPCODE_OFFSET, OPCODE_LENGTH);
      DataInputStream dis = new DataInputStream(opStream);
      short opcode = -1;
      try {
         opcode = dis.readShort();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return opcode;
   }


   
}
