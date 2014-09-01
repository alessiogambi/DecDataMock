package com.tftpsuite.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import com.tftpsuite.message.Message;
import com.tftpsuite.message.MessageType;
import com.tftpsuite.message.RRQMessage;

public class RRQPacket extends TFTPPacket{
   
   private RRQMessage _rrq;
   
   public RRQPacket(RRQMessage rrq, InetAddress address) {
      super(rrq, address);
      _rrq = rrq;
   }
   
   public RRQMessage getRRQMessage() {
      return _rrq;
   }
   
   @Override
   protected byte[] getBytes() {
      ByteArrayOutputStream bs = new ByteArrayOutputStream();
      DataOutputStream ds = new DataOutputStream(bs);
      try {
         ds.writeShort(Message.getMessageTypeCode(MessageType.RRQ));
         ds.flush();
         bs.write(_rrq.getFilename().getBytes("UTF8"));
         bs.write(0);
         bs.write(_rrq.getMode().getBytes("UTF8"));
         bs.write(0);
         bs.flush();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      return bs.toByteArray();
   }
   
}
