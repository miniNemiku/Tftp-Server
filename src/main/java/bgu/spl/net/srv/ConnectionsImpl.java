package bgu.spl.net.srv;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
public class ConnectionsImpl<T> implements Connections<T> {

    // Fields
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> connections; //MAP between connectionId and ConnectionHandler
    private AtomicInteger id = new AtomicInteger(0);
    // Constructor
    public ConnectionsImpl() {
        connections = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
    }

    // Methods
    public void connect(int connectionId, ConnectionHandler<T> handler){
        if(!connections.containsKey(connectionId))
            connections.put(connectionId, handler);
    }

    public boolean send(int connectionId, T msg){
        ConnectionHandler<T> connectionHandler = connections.get(connectionId);
        if(connectionHandler!= null){
            connectionHandler.send(msg);
            return true;
        }
        return false;
    }

    public void disconnect(int connectionId){
            connections.remove(connectionId);
    }

    public ConnectionHandler<T> getConnectionHandler(int connectionId){
        return connections.get(connectionId);
    }

    public int getUniqueId(){
        return id.getAndIncrement();
    }

    public int getId(){
        return id.get();
    }

    public int addNewConnection(ConnectionHandler<T> handler){
        int connectionId = getUniqueId();
        connect(connectionId, handler);
        return connectionId;
    }
}