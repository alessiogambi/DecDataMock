package com.tftpsuite.net;

import com.tftpsuite.message.Message;
import com.tftpsuite.server.MockServer;

/**
 * (to be used by client)
 * @author arifogel
 *
 */
public class MockNet implements INet {

   private MockServer _mockServer;
   
   public MockNet() {
      _mockServer = new MockServer();
   }
   
   @Override
   public void initServer(int sourceTID) {} //does nothing in mock

   @Override
   public void closeServer() {} //does nothing in mock

   @Override
   public Message receiveMessage() {
      return _mockServer.serverSendMessage();
   }

   @Override
   public void sendMessage(Message message) {
      _mockServer.serverReceiveMessage(message);
   }

}
