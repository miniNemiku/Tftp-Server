package bgu.spl.net.impl.tftp.packets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DATA extends PACKET {
    private short block;
    private byte[] data;
    private short packetSize;

    public DATA(short packetSize,short block, byte[] data) {
        super((short)3);
        this.packetSize = packetSize;
        this.block = block;
        this.data = data;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0); // High byte of Opcode
        bytes.write(3); // Low byte of Opcode
        bytes.write((byte)(packetSize >> 8)); // High byte of Packet Size
        bytes.write((byte)packetSize); // Low byte of Packet Size
        bytes.write((byte)(block >> 8)); // High byte of Block number
        bytes.write((byte)block); // Low byte of Block number
    
        // Data
        try {
            bytes.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        return bytes.toByteArray();
    }
}