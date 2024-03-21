package bgu.spl.net.impl.tftp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.file.Files;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.impl.tftp.packets.*;
import bgu.spl.net.srv.Connections;


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    // Fields
    private int connectionId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate;
    private static ConcurrentHashMap<Integer,String> activeUsers = new ConcurrentHashMap<>(); // Mapping between connectionId and usernames. - used in BCAST,LOGRQ,DISC.
    private static ConcurrentHashMap<Integer,File> activeFiles = new ConcurrentHashMap<>(); // Mapping between connectionId and Files. - used in WRQ,DATA,DELRQ.
    private static ConcurrentHashMap<Integer,ByteArrayOutputStream> incomingFiles = new ConcurrentHashMap<>(); // Mapping between connectionId and ByteArrayOutputStream. - used in WRQ,DATA,DELRQ.
    private ConcurrentLinkedQueue<byte[]> packetsToSend;
    private int CurrblockNumber=1;
    private Object lockA = new Object();
    // Methods
    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
        this.shouldTerminate = false;
        this.packetsToSend= new ConcurrentLinkedQueue<>();
    }

    @Override
    public void process(byte[] message) {
        if(message != null){
            short opcode = (short)(((short)message[0]) << 8  | (short)(message[1] & 0x00ff));
            if(opcode != 7 && !activeUsers.containsKey(connectionId)){
                ERROR errorPacket = new ERROR(6);// User not logged in.
                connections.send(connectionId,errorPacket.getBytes());
            }
            else{          
                handleMessage(opcode,message);          
            }
        }
    }

    public void handleMessage(short opcode, byte[] message){
        switch(opcode){
            case 1: // RRQ
                handleRRQ(opcode,message);
                break;

            case 2: // WRQ
                handleWRQ(opcode,message);
                break;

            case 3: // DATA
                handleDATA(opcode,message);
                break;

            case 4: // ACK 
                handleACK(opcode,message);
                break;
                                                                     
            case 5: // ERROR
                ERROR errorPacket1 = new ERROR(4);
                connections.send(connectionId,errorPacket1.getBytes());
                break;

            case 6: // DIRQ
                handleDIRQ(opcode,message);
                break;

            case 7: // LOGRQ -> ERROR PACKET
                handleLOGRQ(opcode,message);
                break;

            case 8: // DELRQ
                handleDELRQ(opcode,message);
                break;

            case 9: // BCAST
                handleBCAST(message);
                break;

            case 10: // DISC
                handleDISC(opcode,message);
                break;

            default:
                // SEND ERROR PACKET
                ERROR errorPacket = new ERROR(4);
                connections.send(connectionId,errorPacket.getBytes());
                break;
        }
    }

    private void handleLOGRQ(short opcode,byte[] message) { 
        byte[] userNameBytes = Arrays.copyOfRange(message, 2, message.length);
        String userName = decodeBytesUntilNull(userNameBytes);
        // First time the client tries to login
        synchronized(activeUsers){
            if(activeUsers.containsKey(connectionId) || activeUsers.containsValue(userName)){ // Name must be unique
            // User already logged in
                ERROR errorPacket = new ERROR(7);
                connections.send(connectionId,errorPacket.getBytes());
                return;
            }else{
                // User is brand new.
                activeUsers.put(connectionId,userName);
                ACK ackPacket = new ACK((short)0);
                connections.send(connectionId,ackPacket.getBytes());
            }
        }
    }

    // User "downloads" a file from the server -> to which directory of the client.
    private void handleRRQ(short opcode, byte[] message) {
        byte[] fileNameBytes = Arrays.copyOfRange(message, 2, message.length);
        String fileName = decodeBytesUntilNull(fileNameBytes);
        Path path = Paths.get("Files", fileName);

        int i=0;
        int countBlock=1;

        if(!Files.exists(path)){
            ERROR error = new ERROR(1);
            connections.send(connectionId, error.getBytes());
        }else{
            try{
                byte[] Alldata = Files.readAllBytes(path); // Read all bytes from the file into a byte array
                while((Alldata.length)-(512*i) >= 512){
                    //packing the first packet
                    byte[] first512Bytes = Arrays.copyOfRange(Alldata, i*512, (i+1)*512);
                    DATA data = new DATA((short)512,(short)countBlock, first512Bytes);
                    packetsToSend.add(data.getBytes());
                    countBlock++;
                    i++;
                }
                if(Alldata.length - 512*i > 0){
                    short remainingSize = (short)(Alldata.length - 512*i);
                    byte[] remainingBytes = Arrays.copyOfRange(Alldata, i*512, Alldata.length);
                    DATA data = new DATA(remainingSize,(short)countBlock, remainingBytes);
                    packetsToSend.add(data.getBytes());
                }
                //Sends the first packet
                connections.send(connectionId, packetsToSend.remove());  
            }catch(IOException e){
                ERROR error = new ERROR(0);
                connections.send(connectionId, error.getBytes());
            }   
        }
        
    }

    // User "uploads" a file to the server
    private void handleWRQ(short opcode, byte[] message) {
        byte[] messageBytes = Arrays.copyOfRange(message, 2, message.length);
        String filename = decodeBytesUntilNull(messageBytes);
        Path path = Paths.get("Files", filename);
        File file = path.toFile();
        if (file.exists()) {
            // File already exists, send ERROR packet
            ERROR error = new ERROR(5);
            connections.send(connectionId, error.getBytes());
        } else {
            // File does not exist, send ACK packet for confirm the action
            ACK ack = new ACK((short)0);
            connections.send(connectionId, ack.getBytes());
            activeFiles.put(connectionId, file);
            incomingFiles.put(connectionId, new ByteArrayOutputStream());
        }
    }
    
    
    private void handleDATA(short opcode, byte[] message) {
        short blockNumber= (short)(((short)message[4] << 8) | ((short)message[5] & 0x00FF));
        byte[] data = Arrays.copyOfRange(message, 6, message.length);
        ByteArrayOutputStream fileData = incomingFiles.get(connectionId);
        try {
            fileData.write(data); // <- Preparing the file.
            ACK ack = new ACK(blockNumber);
            connections.send(connectionId, ack.getBytes());
        } catch (IOException e) {
            ERROR error = new ERROR(3);
            connections.send(connectionId, error.getBytes());
        }

        //Only after receiving less than 512 bytes = end of file, create the file.
        if(data.length < 512){
            Path path = activeFiles.get(connectionId).toPath();
            try {
                Files.write(path, fileData.toByteArray()); // Will create the file
                byte[] messageBcast = new BCAST((short)9,(byte)1,activeFiles.get(connectionId).getName()).getBytes();
                handleBCAST(messageBcast);
            } catch (IOException e) {
                System.out.println("Can't get path");
                ERROR error = new ERROR(3);
                connections.send(connectionId, error.getBytes());
            }
            incomingFiles.remove(connectionId);
        }
    }
    
    private void handleACK(short opcode, byte[] message) {
        short blockNumber = (short)(((short)message[2]) << 8 | (short)(message[3] & 0x00ff));
        if(blockNumber == CurrblockNumber){
            CurrblockNumber++;
            //If is there another packet to send ,continue
            if(!packetsToSend.isEmpty())
                connections.send(connectionId, packetsToSend.remove());
            else
                CurrblockNumber=1;
            
        }else{
            System.out.println("Not the right block number");
            ERROR error = new ERROR(4);
            connections.send(connectionId,error.getBytes());
        }
    }
    
    
    private void handleDIRQ(short opcode,byte[] message) {
    synchronized(lockA){
            File directory = new File("Files/");
            File[] files = directory.listFiles();
            
            int i = 0;
            int countBlock = 1;

            if (files != null) {
                ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            dataBytes.write(file.getName().getBytes(StandardCharsets.UTF_8));
                            dataBytes.write(0); // Separator
                        } catch (IOException e) { // Not expected to happen with that method(write)
                            e.printStackTrace();
                        }
                    }
                }
                // Send DATA packet with maximum of 512 bytes.
                while(dataBytes.size()-512*i >= 512){
                    // Send DATA packet
                    byte[] first512Bytes = Arrays.copyOfRange(dataBytes.toByteArray(), i*512, (i+1)*512);
                    DATA data = new DATA((short)512,(short)countBlock, first512Bytes);
                    packetsToSend.add(data.getBytes());
                    countBlock++;
                    i++;
                }
                    // Send DATA packet
                if(dataBytes.size() - 512*i > 0){
                    short remainingSize = (short)(dataBytes.size() - 512*i);
                    byte[] remainingBytes = Arrays.copyOfRange(dataBytes.toByteArray(), i*512, dataBytes.size());
                    DATA data = new DATA(remainingSize,(short)countBlock, remainingBytes);
                    packetsToSend.add(data.getBytes());
                    countBlock++;
                    i++;
                }  
                //Sends the first packet
                if (!packetsToSend.isEmpty()) {
                    connections.send(connectionId, packetsToSend.remove());
                } else {
                    connections.send(connectionId, new DATA((short)0, (short)1, new byte[0]).getBytes());
                }
            } else {
                // Failed to list files, send ERROR packet
                ERROR error = new ERROR(2); // Error code 2: Access violation
                connections.send(connectionId, error.getBytes());
            }
        }
    }
   
    
    private String decodeBytesUntilNull(byte[] message){
        int i = 0;
        ByteArrayOutputStream filenameBytes = new ByteArrayOutputStream();
        while (i < message.length && message[i] != 0) {
            filenameBytes.write(message[i]);
            i++;
        }
        String filename = null;
        try {
            filename = filenameBytes.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {}
        return filename;
    }

    private void handleDELRQ(short opcode,byte[] message) {
    synchronized(lockA){
            byte[] filenameBytes = Arrays.copyOfRange(message, 2, message.length);
            String filename = decodeBytesUntilNull(filenameBytes);

            // Check if the file exists and delete it
            File fileToDelete = new File("Files/" + filename);
            if (fileToDelete.exists() && fileToDelete.isFile()) {
                if (fileToDelete.delete()) {
                    // Send ACK packet - THE ACTUAL ENCODE
                    ACK ack = new ACK((short)0);
                    connections.send(connectionId, ack.getBytes());
                    activeFiles.remove(connectionId);
                    // Send BCAST packet to all active users.
                    byte[] bcast = new BCAST((short)9, (byte)0, filename).getBytes();
                    handleBCAST(bcast);
                } else {
                    // Failed to delete file, send ERROR packet - THE ACTUAL ENCODE
                    ERROR error = new ERROR(1);
                    connections.send(connectionId, error.getBytes());
                }
            } else {
                // File does not exist, send ERROR packet - THE ACTUAL ENCODE
                ERROR error = new ERROR(1);
                connections.send(connectionId, error.getBytes());
            }
        }
    }

    // There's a slight disagreement here with co-pilot but it's okay.
    private void handleBCAST(byte[] message) {
        // SINCE BCAST is Server to Client message type  - no need to decode it! :)
        // Get all arguments for the BCAST packet
        short opcode = (short)(((short)message[0]) << 8 |(short)(message[1] & 0x00ff));
        byte operation = message[2];
        byte[] filenameBytes = Arrays.copyOfRange(message, 3, message.length);
        String filename = decodeBytesUntilNull(filenameBytes);

        // Create BCAST packet
        BCAST bcast = new BCAST(opcode, operation,filename);

        // Send BCAST packet to all active users - SYNC THEM BEFORE SOMEONE LOGS OUT OR STUFF LIKE THAT
        synchronized(activeUsers){
            for(int activeUser : activeUsers.keySet()){
                connections.send(activeUser,bcast.getBytes());
            }
        }
    }
    
    private void handleDISC(short opcode,byte[] message) {
        ACK ack = new ACK((short)0);
        connections.send(connectionId,ack.getBytes());
        activeUsers.remove(connectionId);
        connections.disconnect(connectionId);
        shouldTerminate = true;
    }
    
    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    } 
}
