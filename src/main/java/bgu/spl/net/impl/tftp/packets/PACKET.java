package bgu.spl.net.impl.tftp.packets;

public class PACKET {
    private short opcode;
    public PACKET(short opCode){this.opcode = opCode;}
    public short getOpCode(){return this.opcode;}
}
