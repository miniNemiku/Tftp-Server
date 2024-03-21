package bgu.spl.net.impl.tftp;
import bgu.spl.net.srv.Server;

public class TftpServer<T>  {

    // Start Server
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        Server.threadPerClient(
                port, //port
                () -> new TftpProtocol(), //protocol factory
                TftpEncoderDecoder::new //message encoder decoder factory
        ).serve();
    }
}
    

