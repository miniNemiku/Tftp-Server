package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.MessageEncoderDecoder;

// import java.io.ByteArrayOutputStream;
// import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 6]; //  start at 64 bits and expand if needed in the pushByte method
    private int len = 0;
    private short dataSize = 0;
    private boolean beenInData=false;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        pushByte(nextByte);
        if(len >= 2){
            short opcode = (short)(((short)bytes[0]) << 8 |(short)(bytes[1]& 0x00ff )); 
            switch(opcode){
                case 1: // RRQ
                case 2: // WRQ
                case 5: // ERROR
                case 7: // LOGRQ 
                case 8: // DELRQ
                case 9: // BCAST
                    if (nextByte == '\0') 
                        return popByte();
                    break;

                case 6: // DIRQ
                case 10: // DISC
                    return popByte();
    
                case 4: // ACK
                    if(len == 4)
                        return popByte();
                    break;
    
                case 3: // DATA
                    if(len >= 6){
                        if(!beenInData){
                            dataSize = (short)(((short)bytes[2]) << 8 |(short)(bytes[3]& 0x00ff));
                            beenInData=true;
                        }
                        else if(beenInData && dataSize > 0){
                            dataSize--;
                        }
                        if(beenInData && dataSize==0)
                            return popByte();
                    }
                    break;
               
    
                default:
                    break;
            }
        }
        return null; // Not a line yet
    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) { // Expand if needed.
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len] = nextByte;
        len++;
    }

    private byte[] popByte() {
        byte[] result = Arrays.copyOf(bytes, len);
        len = 0;
        dataSize = 0;
        beenInData=false;
        return result;
    }
}