package bgu.spl.net.impl.tftp.packets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BCAST extends PACKET {
    private byte operation;
    private String filename;

    public BCAST(short opcode, byte operation, String filename) {
        super((short)9);
        this.operation = operation;
        this.filename = filename;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write(0); // High byte of Opcode
        bytes.write(9); // Low byte of Opcode
        bytes.write(operation);

        // Filename
        try {
            bytes.write(filename.getBytes(StandardCharsets.UTF_8));
            bytes.write(0); // Null terminator
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes.toByteArray();
    }
}