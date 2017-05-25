import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.json.simple.*;
import java.util.Random;
import java.io.DataOutputStream;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * This class contains the main method of the server side.
 * It defines the basic attributes of the server, set initialization and shutting down of 
 * server, handles server command and server interaction(server exchange).
 *
 */
public class EZshareServer {
	static ServerSocket server;
	
	/**key of this hash map is the URI of a resource, value is resource*/
	public HashMap<String, Resource> resources;
	/*public String secret = "12345678";*/
	
	//serverList store unsecure server list;
	public ArrayList<String> serverList;	
	//store secure server list;
	public ArrayList<String> secureServerList;
	
	public static String commandType;
	public static boolean hasDebugOption = false;
	public static String command = "";
	public static String hostName = "";
	public static int connectionintervallimit;
	public static int port = 3780;
	public static int sport = 3781;
	public static String secret = "";
	public static int exchangeInterval = 0;
	public boolean isSecure;
	public static boolean isSecurePort = false;
	static HashMap connectionInterval = new HashMap<Socket,java.util.Date>();
	
	/**Constructor for secure connection EZshareServer*/
	public EZshareServer(boolean isSecure){
		this.resources = new HashMap<String, Resource>();
		this.serverList = new ArrayList<String>();
		this.secureServerList = new ArrayList<String>();
		this.isSecure = isSecure;
	};
	
	public EZshareServer(int serverPort,boolean isSecure) {
		try {
			this.server  = new ServerSocket(serverPort);
			this.resources = new HashMap<String, Resource>();
			this.serverList = new ArrayList<String>();
			this.isSecure = isSecure;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	
	/**
	 * This method handles input commands on server.
	 * @param args
	 */
	public static void handleServerInput(String[] args){
		
		//when the input command contains "-debug", handle the args[] to better use options.
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-debug")){
			hasDebugOption=true;
	    		String[] argsWithDebug = new String[args.length+1];
	    		argsWithDebug[i+1] = "";
	    		System.arraycopy(args, 0, argsWithDebug, 0, i+1);
	    		System.arraycopy(args, i+1, argsWithDebug, i+2, args.length-1-i);
	    		args = new String [args.length+1];
	    		System.arraycopy(argsWithDebug, 0, args, 0, argsWithDebug.length);
	    		break;
			}
		}
		

		
		//define server command options.
		Options options = new Options();
		options.addOption("exchangeinterval",true,"input exchange interval"); 
		options.addOption("advertisedhostname",true, "input host");
	    options.addOption("port",true, "input port");
	    options.addOption("connectionintervallimit",true, "input interval");
	    options.addOption("secret",true, "input secret");
	    options.addOption("debug",true,"debug option");
	    options.addOption("sport",true,"input secure port number");
	    
	    
	    CommandLineParser parser = new DefaultParser();
	    CommandLine cmd = null;
	    try{
	        cmd = parser.parse(options,args);      
	    } 
	    catch (org.apache.commons.cli.ParseException e) {
			e.printStackTrace();
		}
	    
	    if(cmd.hasOption("exchangeinterval")){
	    	exchangeInterval = Integer.parseInt(cmd.getOptionValue("exchangeinterval"))*1000;
	    }else{
	    	exchangeInterval = 1000*60*10; 
	    }
	    
	    if (cmd.hasOption("secret")) {
			secret = cmd.getOptionValue("secret");
		}else{
			//random string as  default secret
			Random random = new Random();
			secret = RandomStringUtils.randomAlphabetic(20);
			System.out.println("random secret as default: "+secret);
		}
	    
	    if(cmd.hasOption("advertisedhostname")){
	    	hostName = cmd.getOptionValue("advertisedhostname");
	    }else{
	    	try {
				hostName = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    if(cmd.hasOption("port")){
	    	port =Integer.parseInt(cmd.getOptionValue("port"));
	    }
	    
	    if(cmd.hasOption("connectionintervallimit")){
	    	connectionintervallimit = Integer.parseInt(cmd.getOptionValue("connectionintervallimit"))*1000;
	    }else{
	    	//default 1000ms 
	    	connectionintervallimit = 1000;
	    }
	    
	    //secure port option
	    if(cmd.hasOption("sport")){
	    	sport = Integer.parseInt(cmd.getOptionValue("sport"));
	    	isSecurePort = true;
	    }
	    
	}
	
	/**
	 * The main method to run the server.
	 * @param args
	 */
	public static void main(String[] args){
		System.out.println("Welcome to the EZShare");
		handleServerInput(args);
		initializeServer();
		
		/*initializeServer(3780);*/
	}
	
	/**
	 * This method is the finalization of the server.
	 */
	protected void finalize() throws Throwable{
		try {
				System.out.println("Server shutdown");
				server.close();
		}finally {
				super.finalize();
		}
	}
	
	/**
	 * This method is the initialization of the server.
	 */
	public static void initializeServer(){
		try {
			/**add server itself in the serverlist for exchange interaction*/
			
			if(isSecurePort){
				//Secure port initiated, only received secure connection
				
				//keystore contains our own certificate and private key
				System.setProperty("javax.net.ssl.trustStore", "sslconnection/serverKeystore.jks");
				System.setProperty("javax.net.ssl.keyStore","sslconnection/serverKeystore.jks");
				
				//password to access the private key from the keystore file
				System.setProperty("javax.net.ssl.keyStorePassword", "12345678");
				
				// Enable debugging to view the handshake and communication which happens between the SSLClient and the SSLServer
				//System.setProperty("javax.net.debug","all");
				
				EZshareServer eZshareServer = new EZshareServer(isSecurePort);
				
				
				try{
					SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
							.getDefault();
					SSLServerSocket sslServerSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(sport);
					
					Integer localPort = sport;
					String localHost = InetAddress.getLocalHost().getHostAddress();
					eZshareServer.secureServerList.add(localHost+":"+localPort.toString());
					
					/**every 10 mins(by default), contact a randomly selected server in the server list*/
					Timer timer = new Timer();		
					timer.schedule(new ExchangeTask(eZshareServer,hasDebugOption), exchangeInterval,exchangeInterval);
					
					int count = 0;
					long temp=0;
					while(true){
						SSLSocket sslClientSocket = (SSLSocket) sslServerSocket.accept();
						java.util.Date currentTime = new java.util.Date();
						
						/*handle interval limit*/
						long threadTime = currentTime.getTime();
						count = count +1;
						System.out.println("client "+count+" applying for connection");
						if(count!=1){
							if(threadTime-temp<connectionintervallimit){
								System.out.println("violate interval limit");
								temp = threadTime;
							}else {
								ServerThread thread = new ServerThread(sslClientSocket, eZshareServer.resources, eZshareServer.secret, sslServerSocket,
										eZshareServer.serverList,eZshareServer.secureServerList, eZshareServer.hasDebugOption, connectionintervallimit,hostName,eZshareServer.isSecure);
								thread.start();
								temp = threadTime;
							}
						}else{
							ServerThread thread = new ServerThread(sslClientSocket, eZshareServer.resources, eZshareServer.secret, sslServerSocket,
									eZshareServer.serverList, eZshareServer.secureServerList,eZshareServer.hasDebugOption, connectionintervallimit,hostName,eZshareServer.isSecure);
							thread.start();
							temp = threadTime;
						}
							
					}
					
				
				} catch (Exception exception) {
					exception.printStackTrace();
				}
				
			}else{
				//Unsecured port established
				EZshareServer eZshareServer = new EZshareServer(port,isSecurePort);
				
				String localHost = InetAddress.getLocalHost().getHostAddress();
				Integer port = eZshareServer.port;
				String localPort = port.toString();
				eZshareServer.serverList.add(localHost+":"+localPort);
			
				/**every 10 mins(by default), contact a randomly selected server in the server list*/
				Timer timer = new Timer();		
				timer.schedule(new ExchangeTask(eZshareServer,hasDebugOption), exchangeInterval,exchangeInterval);
				
				int count = 0;
				long temp=0;
				while(true){
					Socket client = EZshareServer.server.accept();
					java.util.Date currentTime = new java.util.Date();
					
					/*handle interval limit*/
					long threadTime = currentTime.getTime();
					count = count +1;
					System.out.println("client "+count+" applying for connection");
					if(count!=1){
						if(threadTime-temp<connectionintervallimit){
							System.out.println("violate interval limit");
							temp = threadTime;
						}else {
							String hostAndPort = eZshareServer.server.getInetAddress().getHostAddress() + ":" + eZshareServer.server.getLocalPort();
							ServerThread thread = new ServerThread(client, eZshareServer.resources, eZshareServer.secret, hostAndPort,
									eZshareServer.serverList, eZshareServer.hasDebugOption, connectionintervallimit,hostName,eZshareServer.isSecure);
							thread.start();
							temp = threadTime;
						}
					}else{
						String hostAndPort = eZshareServer.server.getInetAddress().getHostAddress() + ":" + eZshareServer.server.getLocalPort();
						ServerThread thread = new ServerThread(client, eZshareServer.resources, eZshareServer.secret, hostAndPort,
								eZshareServer.serverList, eZshareServer.hasDebugOption, connectionintervallimit,hostName,eZshareServer.isSecure);
						thread.start();
						temp = threadTime;
					}
						
				}
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	
	
	
	

}
