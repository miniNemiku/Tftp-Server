package bgu.spl.net.impl.tftp.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ERROR extends PACKET{

    private int value;
    public ERROR (int value){
        super((short)5);
        this.value = value;
    }
    public byte[] getBytes(){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0); // High byte of Opcode
        bytes.write(5); // Low byte of Opcode
        bytes.write(0); // High byte of Error code
        bytes.write(value); // Low byte of Error code

        // Error message
        String errMsg;
        switch (value) {
            case 1:
                errMsg = "File not found - RRQ DELRQ of non-existing file.";
                break;
            case 2:
                errMsg = "Access violation - File cannot be written, read or deleted.";
                break;
            case 3:
                errMsg = "Disk full or allocation exceeded - No room in disk.";
                break;
            case 4:
                errMsg = "Illegal TFTP operation - Unknown Opcode.";
                break;
            case 5:
                errMsg = "File already exists - File name exists on WRQ.";
                break;
            case 6:
                errMsg = "User not logged in - Any opcode received before Login completes.";
                break;
            case 7:
                errMsg = "User already logged in - Login username already connected.";
                break;
            default: // Case 0 (and others numbers but no other numbers should be used.)
                errMsg = "Not defined";
                break;
        }
        try {
            bytes.write(errMsg.getBytes(StandardCharsets.UTF_8));
            bytes.write(0); // Null terminator
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes.toByteArray();
    }
}
