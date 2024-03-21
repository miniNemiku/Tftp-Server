package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private Object lock = new Object();
    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { // Just for automatic closing
            int read;
            
            in = new BufferedInputStream(sock.getInputStream()); // READ 
            out = new BufferedOutputStream(sock.getOutputStream()); // WRITE
            // I'm connected + I have something to read + protocol is still running (in.read = one byte from the input stream, -1 if the end of the stream is reached.)
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read); 
                if (nextMessage != null) { //As long as there's another byte to read
                    protocol.process(nextMessage);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();

    }

    @Override
    public void send(T msg) {
        synchronized (lock) {
            try{
                out.write(encdec.encode(msg));
                out.flush();
            } catch (IOException e) {e.printStackTrace();}
        }
    }
}
