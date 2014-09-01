package com.tftpsuite.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.tftpsuite.message.ACKMessage;
import com.tftpsuite.message.DATAMessage;
import com.tftpsuite.message.ERRORMessage;
import com.tftpsuite.message.ErrorType;
import com.tftpsuite.message.Message;
import com.tftpsuite.message.RRQMessage;
import com.tftpsuite.message.WRQMessage;
import com.tftpsuite.net.INet;
import com.tftpsuite.net.Net;
import com.tftpsuite.net.TID;

public class TFTPClient {

   private static final String TRANSFER_MODE = "octet";
   
   
   private String _filename;
   private Action _action;
   private short _expectedBlockNum;
   private INet _net;
   
   public TFTPClient(String filename, Action action, INet net) {
      _filename = filename;
      _action = action;
      _net = net;
   }
   
   public void act() throws IOException {
      switch (_action) {
         case READ:
            ReadFile();
            break;
            
         case WRITE:
            WriteFile();
            break;
            
         default:
            break;
      }
   }
   
   private void WriteFile() throws IOException {
      RandomAccessFile raf;
      raf = new RandomAccessFile(_filename, "r");
      long fileSize;
      fileSize = raf.length();
      int numBlocks = (int)(fileSize / (long)(DATAMessage.MAX_DATA_SIZE) + 1L);
      
      int sourceTID = TID.generateTID();
      int destinationTID = -1;
      _net.initServer(sourceTID);
      _expectedBlockNum = 0;
      WRQMessage wrq = new WRQMessage(_filename, TRANSFER_MODE, sourceTID, TID.INITIAL_SERVER_TID);
      _net.sendMessage(wrq);
      ERRORMessage em;
      while (true) {
         Message recvMsg = _net.receiveMessage();
         switch (recvMsg.getMessageType()) {
            case ACK:
               if (destinationTID == -1) {
                  destinationTID = recvMsg.getSourceTID();
               }
               else if (destinationTID != recvMsg.getSourceTID()) {
                  em = new ERRORMessage(
                        ErrorType.UNKOWN_TRANSFER_ID,
                        ERRORMessage.getErrorTypeMessage(ErrorType.UNKOWN_TRANSFER_ID),
                        sourceTID,
                        destinationTID);
                  System.err.println(em.getErrorMessage());
                  _net.sendMessage(em);
                  continue;
               }
               ACKMessage ackMessage = (ACKMessage)recvMsg;
               short recvBlockNum = ackMessage.getBlockNum();
               
               //terminate if final ack received
               if (recvBlockNum == numBlocks) {
                  _net.closeServer();
                  raf.close();
                  return;
               }
               else if (recvBlockNum > numBlocks || recvBlockNum > _expectedBlockNum) {
                  em = new ERRORMessage(
                        ErrorType.ILLEGAL_TFTP_OPERATION,
                        ERRORMessage.getErrorTypeMessage(ErrorType.ILLEGAL_TFTP_OPERATION),
                        sourceTID,
                        destinationTID);
                  System.err.println(em.getErrorMessage());
                  _net.sendMessage(em);
                  _net.closeServer();
                  return;
               }
               
               //increment block counter if appropriate
               if (recvBlockNum == _expectedBlockNum) {
                  _expectedBlockNum++;
               }

               //read data if appropriate
               long currentOffset = recvBlockNum * DATAMessage.MAX_DATA_SIZE;
               byte[] currentData = new byte[(int)Math.min(
                     DATAMessage.MAX_DATA_SIZE,
                     fileSize - currentOffset)];
               raf.seek(currentOffset);
               raf.read(
                     currentData,
                     0,
                     currentData.length);
               //send data
               DATAMessage dm = new DATAMessage(currentData, (short)(recvBlockNum + 1), sourceTID, destinationTID);
               _net.sendMessage(dm);
               break;
            
            case ERROR:
               em = (ERRORMessage)recvMsg;
               System.err.println(em.getErrorMessage());
               _net.closeServer();
               return;
               
            default:
               em = new ERRORMessage(
                     ErrorType.ILLEGAL_TFTP_OPERATION,
                     ERRORMessage.getErrorTypeMessage(ErrorType.ILLEGAL_TFTP_OPERATION),
                     sourceTID,
                     destinationTID);
               _net.sendMessage(em);
               System.err.println(em.getErrorMessage());
               _net.closeServer();               
               return;
         }
      }
   }

   private void ReadFile() throws IOException {
      FileOutputStream fos;
      fos = new FileOutputStream(_filename);
      int sourceTID = TID.generateTID();
      int destinationTID = -1;
      _net.initServer(sourceTID);
      _expectedBlockNum = 1;
      RRQMessage rrq = new RRQMessage(_filename, TRANSFER_MODE, sourceTID, TID.INITIAL_SERVER_TID);
      _net.sendMessage(rrq);
      ERRORMessage em;
      while (true) {
         Message recvMsg = _net.receiveMessage();
         System.out.println("Client received message: " + recvMsg);
         switch (recvMsg.getMessageType()) {
            case DATA:
               if (destinationTID == -1) {
                  destinationTID = recvMsg.getSourceTID();
               }
               else if (destinationTID != recvMsg.getSourceTID()) {
                  em = new ERRORMessage(
                        ErrorType.UNKOWN_TRANSFER_ID,
                        ERRORMessage.getErrorTypeMessage(ErrorType.UNKOWN_TRANSFER_ID),
                        sourceTID,
                        destinationTID);
                  System.err.println(em.getErrorMessage());
                  _net.sendMessage(em);
                  continue;
               }
               DATAMessage dm = (DATAMessage)recvMsg;
               byte[] currentData = dm.getData();
               if (currentData.length > DATAMessage.MAX_DATA_SIZE) {
                  em = new ERRORMessage(
                        ErrorType.UNDEFINED,
                        "DATA payload too large (>" + DATAMessage.MAX_DATA_SIZE + " bytes)",
                        sourceTID,
                        destinationTID);
                  _net.sendMessage(em);
                  System.err.println(em.getErrorMessage());
                  _net.closeServer();
                  return;
               }
               short recvBlockNum = dm.getBlockNum();
               
               //send acknowledgment
               ACKMessage am = new ACKMessage(recvBlockNum, sourceTID, destinationTID);
               _net.sendMessage(am);
               
               //write data if appropriate
               if (recvBlockNum == _expectedBlockNum) {
                  _expectedBlockNum++;
                  fos.write(currentData);
               }
               
               //terminate if data block is not saturated
               if (currentData.length < DATAMessage.MAX_DATA_SIZE) {
                  fos.close();
                  return;
               }
               break;
            
            case ERROR:
               em = (ERRORMessage)recvMsg;
               System.err.println(em.getErrorMessage());
               _net.closeServer();
               return;
               
            default:
               em = new ERRORMessage(
                     ErrorType.ILLEGAL_TFTP_OPERATION,
                     ERRORMessage.getErrorTypeMessage(ErrorType.ILLEGAL_TFTP_OPERATION),
                     sourceTID,
                     destinationTID);
               _net.sendMessage(em);
               System.err.println(em.getErrorMessage());
               _net.closeServer();               
               return;
         }
      }
   }

   private static String USAGE_STRING =
         "Usage: <executable> <hostname> <filename> <r|w>";
   
   /**
    * @param args
    */
   public static void main(String[] args) {
      if (args.length != 3) {
         System.err.println(USAGE_STRING);
         return;
      }
      
      String host = args[0];
      String filename = args[1];
      String actionString = args[2];
      Action action;
      
      if (actionString.equals("r")) {
         action = Action.READ;
      }
      else if (actionString.equals("w")) {   
         action = Action.WRITE;
      }            
      else {
         System.err.println("Invalid action specification: " + actionString);
         System.err.println(USAGE_STRING);
         return;
      }
      
      TFTPClient client = new TFTPClient(filename, action, new Net(host));
      try {
         client.act();
      }
      catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }

}
