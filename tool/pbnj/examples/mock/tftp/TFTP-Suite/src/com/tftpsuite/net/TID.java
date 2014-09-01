package com.tftpsuite.net;

import java.util.Random;

public class TID {
   public static final int INITIAL_SERVER_TID = 69;
   
   private static final int MIN_EPHEMERAL_PORT = 49152;
   private static final int MAX_EPHEMERAL_PORT = 61000;
   private static Random _random = new Random();
   
   
   public static int generateTID() {
      return _random.nextInt(MAX_EPHEMERAL_PORT - MIN_EPHEMERAL_PORT) + MIN_EPHEMERAL_PORT;
   }
   

}
