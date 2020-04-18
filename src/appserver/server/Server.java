package appserver.server;

import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.comm.ConnectivityInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import utils.PropertyHandler;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Server {

    // Singleton objects - there is only one of them. For simplicity, this is not enforced though ...
    static SatelliteManager satelliteManager = null;
    static LoadManager loadManager = null;
    static ServerSocket serverSocket = null;

    public Server(String serverPropertiesFile) {
        int port;
        
        // create satellite manager and load manager
        // ...
        satelliteManager = new SatelliteManager();
        System.out.println("[Server] set up SatelliteManager");
        
        loadManager = new LoadManager();
        System.out.println("[Server] set up LoadManager");
        
        // read server properties and create server socket
        try
        {
            PropertyHandler appServerProps = new PropertyHandler(serverPropertiesFile);
            port = Integer.parseInt(appServerProps.getProperty("PORT"));
            serverSocket = new ServerSocket(port);
            System.out.println("[Server] successfully set up ServerSocket.");
        }    
        catch(IOException e)
        {
            System.err.println(e);
        }
    }

    public void run() {
    // serve clients in server loop ...
    // when a request comes in, a ServerThread object is spawned
    // ...
        while(true)
        {
            try
            {
                Socket socket = serverSocket.accept();
                (new ServerThread(socket)).start();
            }
            catch(IOException e)
            {
                System.err.println(e);
            }
        }
    }

    // objects of this helper class communicate with satellites or clients
    private class ServerThread extends Thread {

        Socket client = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        private ServerThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            String satelliteName;
            ConnectivityInfo satelliteInfo;
            // set up object streams and read message
            System.out.println("[Server] starting new thread...");
            try
            {
                writeToNet = new ObjectOutputStream(client.getOutputStream());
                readFromNet = new ObjectInputStream(client.getInputStream());
            }
            catch(IOException e)
            {
                System.err.println(e);
            }  

            
            // read initial registration message
            try
            {
                message = (Message) readFromNet.readObject();
            }
            catch(IOException | ClassNotFoundException e)
            {
                System.err.println(e);
            }
            
            // process message
            switch (message.getType()) {
                case REGISTER_SATELLITE:
                    // read satellite info
                    satelliteInfo = (ConnectivityInfo) message.getContent();
                    satelliteName = satelliteInfo.getName();
                    System.out.println("[ServerThread.run] Registering satellite " + satelliteName);
                    
                    // register satellite
                    synchronized (Server.satelliteManager) {
                        Server.satelliteManager.registerSatellite(satelliteInfo);
                    }

                    // add satellite to loadManager
                    synchronized (Server.loadManager) {
                        Server.loadManager.satelliteAdded(satelliteName);
                    }

                    break;

                case JOB_REQUEST:
                    System.err.println("\n[ServerThread.run] Received job request");

                    satelliteName = null;
                    synchronized (Server.loadManager) {
                        // get next satellite from load manager
                        try
                        {
                            satelliteName = Server.loadManager.nextSatellite();
                        }
                        catch(Exception e)
                        {
                            System.out.println(e);
                        }
                        
                        // get connectivity info for next satellite from satellite manager
                        satelliteInfo = Server.satelliteManager.getSatelliteForName(satelliteName);
                    }

                    Socket satellite = null;
                    
                    // connect to satellite
                    try
                    {
                        System.out.println("[Sever.run] Running job with satellite: " + satelliteInfo.getName());
                        satellite = new Socket(satelliteInfo.getHost(), satelliteInfo.getPort());
                        
                        // open object streams,
                        ObjectOutputStream writeToSat = new ObjectOutputStream(satellite.getOutputStream());
                        ObjectInputStream readFromSat = new ObjectInputStream(satellite.getInputStream());
                        
                        // forward message (as is) to satellite,
                        writeToSat.writeObject(message);
                        
                        // receive result from satellite and
                        Object result = readFromSat.readObject();
                        
                        // write result back to client
                        writeToNet.writeObject(result);
                    }
                    catch(IOException | ClassNotFoundException e)
                    {
                        System.out.println(e);
                    }

                    break;

                default:
                    System.err.println("[ServerThread.run] Warning: Message type not implemented");
            }
        }
    }

    // main()
    public static void main(String[] args) {
        // start the application server
        Server server = null;
        if(args.length == 1) {
            server = new Server(args[0]);
        } else {
            server = new Server("../../config/Server.properties");
        }
        server.run();
    }
}
