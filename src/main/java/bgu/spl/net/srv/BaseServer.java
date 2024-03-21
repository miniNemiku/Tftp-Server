package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private ConnectionsImpl<T> connections;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        connections = new ConnectionsImpl<>();
    }

    @Override
            public void serve() {
                try (ServerSocket serverSock = new ServerSocket(port)) {
                    System.out.println("Server started");
                    this.sock = serverSock; //just to be able to close
                    while (!Thread.currentThread().isInterrupted()) {
                        BidiMessagingProtocol<T> protocol = protocolFactory.get();
                        Socket clientSock = serverSock.accept();
                        BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(clientSock, encdecFactory.get(), protocol);
                        int conId = connections.addNewConnection(handler);
                        protocol.start(conId, connections);
                        execute(handler);
                    }
                } catch (IOException ex) {}
                System.out.println("server closed!!!");
            }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);
}
