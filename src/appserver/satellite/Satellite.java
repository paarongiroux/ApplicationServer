package appserver.satellite;

import appserver.job.Job;
import appserver.comm.ConnectivityInfo;
import appserver.job.UnknownToolException;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.job.Tool;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.PropertyHandler;

/**
 * Class [Satellite] Instances of this class represent computing nodes that execute jobs by
 * calling the callback method of tool a implementation, loading the tool's code dynamically over a network
 * or locally from the cache, if a tool got executed before.
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Satellite extends Thread {

    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null;
    private Hashtable<String, Tool> toolsCache = null;

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) {
        // read this satellite's properties and populate satelliteInfo object,
        // which later on will be sent to the server
        try
        {
          // initalize the satelite information by reading a property file
            PropertyHandler satProps = new PropertyHandler(satellitePropertiesFile);
            satelliteInfo.setName(satProps.getProperty("NAME"));
            satelliteInfo.setPort(Integer.parseInt(satProps.getProperty("PORT")));
            System.out.println("[Satellite] successfully set up satellite properties.");
        }
        catch(IOException e)
        {
            System.err.println("Error in obtaining satellite propertie");
            System.exit(1);
        }


        // read properties of the application server and populate serverInfo object
        // other than satellites, the as doesn't have a human-readable name, so leave it out
        try
        {
          // initialize the main server information by reading the properties
            PropertyHandler appServerProps = new PropertyHandler(serverPropertiesFile);
            serverInfo.setHost(appServerProps.getProperty("HOST"));
            serverInfo.setPort(Integer.parseInt(appServerProps.getProperty("PORT")));
            System.out.println("[Satellite] successfully set up app server properties.");
        }
        catch(IOException e)
        {
            System.err.println("Error in obtaining server propertie");
            System.exit(1);

        }

        // read properties of the code server and create class loader
        // -------------------
        try
        {
          // initialize the code server information
            PropertyHandler classLoaderProps = new PropertyHandler(classLoaderPropertiesFile);
            String host = classLoaderProps.getProperty("HOST");
            int port = Integer.parseInt(classLoaderProps.getProperty("PORT"));
            classLoader = new HTTPClassLoader(host, port);
            System.out.println("[Satellite] successfully set up class loader.");

        }
        catch(IOException e)
        {
            System.err.println(e);
        }

        if (classLoader == null) {
            // The property file could not be read
            System.err.println("Cannot create HTTPClassLoader");
            System.exit(1);
        }


        // create tools cache
        // cached filled when requesting a class
        // used when client connects
        toolsCache = new Hashtable<String, Tool>();

    }

    @Override
    public void run() {

        // register this satellite with the SatelliteManager on the server
        // ---------------------------------------------------------------

        Message registerMessage = new Message(REGISTER_SATELLITE, satelliteInfo);
        try
        {

            Socket server = new Socket(serverInfo.getHost(), serverInfo.getPort());
            ObjectOutputStream toServer = new ObjectOutputStream(server.getOutputStream());
            System.out.println("[Satellite.run] registering satellite with server");
            toServer.writeObject(registerMessage);
        }
        catch(IOException e)
        {
            System.err.println(e);
        }


        // create server socket with port in properties
        // ---------------------------------------------------------------
        try
        {
            ServerSocket serverSocket = new ServerSocket(satelliteInfo.getPort());
            System.out.println("[Satellite] successfully set up ServerSocket.");
            // start taking job requests in a server loop
            // ---------------------------------------------------------------
            while (true)
            {
                Socket socket = serverSocket.accept();
                (new SatelliteThread(socket, this)).start();
            }
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
    }

    // inner helper class that is instanciated in above server loop and processes single job requests
    private class SatelliteThread extends Thread {

        Satellite satellite = null;
        Socket jobRequest = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        SatelliteThread(Socket jobRequest, Satellite satellite) {
            this.jobRequest = jobRequest;
            this.satellite = satellite;
        }

        @Override
        public void run() {
            System.out.println("[SatelliteThread] starting new thread...");
            // setting up object streams
            try
            {
                writeToNet = new ObjectOutputStream(jobRequest.getOutputStream());
                readFromNet = new ObjectInputStream(jobRequest.getInputStream());
            }
            catch(IOException e)
            {
                System.err.println(e);
            }


            // reading message from the socket to do a job
            try
            {
                message = (Message) readFromNet.readObject();
            }
            catch(ClassNotFoundException | IOException e)
            {
                System.err.println(e);
            }

            // switches to correspond to the job
            switch (message.getType()) {
              // corresponds to the job request
                case JOB_REQUEST:
                    // processing job request
                    Job job = (Job) message.getContent();
                    try
                    {
                        // lookup the tool name corresponding to the job
                        Tool tool = getToolObject(job.getToolName());
                      // run the tool and process the job
                        Object result = tool.go(job.getParameters());

                        // send the result back
                        writeToNet.writeObject(result);

                    }
                    catch (Exception e)
                    {
                        System.err.println(e);
                    }

                    break;

                default:
                    System.err.println("[SatelliteThread.run] Warning: Message type not implemented");
            }
        }
    }

    /**
     * Aux method to get a tool object, given the fully qualified class string
     * If the tool has been used before, it is returned immediately out of the cache,
     * otherwise it is loaded dynamically
     */
    public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Tool toolObject = null;

        // try and get the tool from the toolsCache / if the tool is not cached yet
        if ((toolObject = toolsCache.get(toolClassString)) == null)
        {
            // if the tool wasn't in the cache, load it with the classLoader
            Class toolClass = classLoader.loadClass(toolClassString);
            // create a new Tool instance
            toolObject = (Tool) toolClass.newInstance();
            // put the tool into the toolsCache for future re-use
            toolsCache.put(toolClassString, toolObject);
        }

        // return the Tool object
        return toolObject;
    }

    public static void main(String[] args) {
        // start the satellite
        Satellite satellite = new Satellite(args[0], args[1], args[2]);
        satellite.run();
    }
}
