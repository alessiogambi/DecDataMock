package com.tftpsuite.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.tftpsuite.message.ACKMessage;
import com.tftpsuite.message.DATAMessage;
import com.tftpsuite.message.ERRORMessage;
import com.tftpsuite.message.Message;
import com.tftpsuite.message.MessageType;
import com.tftpsuite.message.RRQMessage;
import com.tftpsuite.message.WRQMessage;

public class Net implements INet {
   private static final int PACKET_BUFFER_SIZE = 2048;
   private DatagramSocket _dsocket;
   private int _sourceTID;
   private InetAddress _address;
   
   public Net(String serverHostname) {
      try {
         _address = InetAddress.getByName(serverHostname);
      }
      catch (UnknownHostException e) {
         e.printStackTrace();
      }
   }
   
   public void initServer(int sourceTID) {
      try {
         _sourceTID = sourceTID;
         _dsocket = new DatagramSocket(_sourceTID);
      }
      catch (SocketException e) {
         e.printStackTrace();
      }
   }
   
   public void closeServer() {
      _dsocket.close();
   }

   @Override
   public Message receiveMessage() {
      byte[] recvBuffer = new byte[PACKET_BUFFER_SIZE];
      DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
      try {
         _dsocket.receive(recvPacket);
      }
      catch (IOException e) {
         e.printStackTrace();
      }

      short opcode = TFTPPacket.parseOpcode(recvPacket.getData());
      MessageType messageType = Message.getMessageTypeFromCode(opcode);
      
      switch (messageType) {
         case ACK:
            return ACKPacket.extractACKMessage(recvPacket, _dsocket.getPort());
            
         case DATA:
            return DATAPacket.extractDATAMessage(recvPacket, _dsocket.getPort());
            
         case ERROR:
            return ERRORPacket.extractERRORMessage(recvPacket, _dsocket.getPort());
            
         case RRQ:
            return null;
            
         case WRQ:
            return null;
            
         default:
            return null;
      }
   }

   @Override
   public void sendMessage(Message message) {
      DatagramPacket dp = null;
      switch (message.getMessageType()) {
         case ACK:
            dp = new ACKPacket((ACKMessage)message, _address).toDatagramPacket();
            break;
            
         case DATA:
            dp = new DATAPacket((DATAMessage)message, _address).toDatagramPacket();
            break;
            
         case ERROR:
            dp = new ERRORPacket((ERRORMessage)message, _address).toDatagramPacket();
            break;
            
         case RRQ:
            dp = new RRQPacket((RRQMessage)message, _address).toDatagramPacket();
            break;
            
         case WRQ:
            dp = new WRQPacket((WRQMessage)message, _address).toDatagramPacket();
            break;
            
         default:
            break;
      }
      try {
         _dsocket.send(dp);
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }
   
}
