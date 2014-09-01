package com.tftpsuite.net;

import com.tftpsuite.message.Message;

public interface INet {
   public void initServer(int sourceTID);
   public void closeServer();
   public Message receiveMessage();
   public void sendMessage(Message message);
}
