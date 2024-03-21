package bgu.spl.net.impl.tftp.packets;

public class ACK extends PACKET {

    private short block;

    public ACK(short block){
        super((short)4);
        this.block = block;
    }
    

    public short getBlock(){
        return block;
    }

    public byte[] getBytes(){
        return new byte[]{(byte)0,(byte)4,(byte)(block >> 8),(byte)block};
    }

}
