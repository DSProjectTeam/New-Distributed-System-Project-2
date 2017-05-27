import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
//import org.json.simple.JSONArray;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * This class is the only class of client.
 * This class creats the client, it handles users' input, convert it to JSON message and send to server.
 * it also receive responses from server and print them out.
 * @author Zizhe Ruan, Bowen Rao.
 */
public class Client {
	//for convenience, ip address is also shown as "host" here.
	public static String host = "localhost";
	public static String host2 = "192.168.1.110";
//	public static String host = "10.12.187.20";
	public static int port = 3781;
	public static int sport = 3781;
	public static String commandType;
	public static boolean hasDebugOption;
	public static boolean hasSecureOption;
	private static boolean enterDetected;
	public static String id = "";

	/**
	 * main method to run client
	 * @param args
	 */
	public static void main(String[] args){
		try {
			commandType = "";
			hasDebugOption = false;
			hasSecureOption = false;
			JSONObject userInput = handleClientInput(args);
			StopWatch swatch = new StopWatch();
			SSLSocket sslsocket = null;
			Socket socket = null;
			DataInputStream in;
			DataOutputStream out;
			
			//set socket to connect to. If input contains secure flag, use secure socket.
			if(hasSecureOption){
				//Location of the Java keystore file containing the collection of 
				//certificates trusted by this application (trust store).
				System.setProperty("javax.net.ssl.trustStore", "sslconnection/clientKeystore.jks");
				System.setProperty("javax.net.ssl.keyStore", "sslconnection/clientKeystore.jks");
				System.setProperty("javax.net.ssl.keyStorePassword", "12345678");
				//System.setProperty("javax.net.debug","all");
				//Create SSL socket and connect it to the remote server 
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				sslsocket = (SSLSocket) sslsocketfactory.createSocket(host, sport);
				sslsocket.setSoTimeout(1300);
				out = new DataOutputStream(sslsocket.getOutputStream());
				in = new DataInputStream(sslsocket.getInputStream());
			}
			else{// no secure flag, create insecure socket.
				socket = new Socket(host,port);
				out = new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			}
			
			//send userInput to server in JSON format.
			out.writeUTF(userInput.toJSONString());
			out.flush();
			
			//print part of debug information.
			if(hasDebugOption){
			    System.out.println("-setting debug on");
			    System.out.println(commandType+" to "+host+":"+port);
				System.out.println("SENT: "+userInput.toJSONString());
			}
			
			//while ENTER not pressed, listen for incoming response from server.
			switch(commandType){
			case "-subscribe": 
				enterDetected = false;
				startEnterListen();
				while(true){
					//when ENTER detected, send out UNSUBSCRIBE command, then close connection
					if(enterDetected==true){
						JSONObject unsubscribeMessage = new JSONObject();
						unsubscribeMessage.put(ConstantEnum.CommandType.command.name(),"UNSUBSCRIBE");
						unsubscribeMessage.put(ConstantEnum.CommandArgument.id.name(),id);
						out.writeUTF(unsubscribeMessage.toJSONString());
						out.flush();
						if(hasDebugOption){
						    System.out.println("-unsubscribing to "+host+":"+port);
							System.out.println("SENT: "+unsubscribeMessage.toJSONString());
						}
						swatch.start();
						while(true){
							/*receive the last message of total resultSize, then close. 
							If not received in 1.3s, disconnect.*/
							if(!hasSecureOption){
								if(in.available()>0){
									String responseMessage = in.readUTF();
									handleServerResponse(userInput, responseMessage, in);
									break;
								}
							}
							else{
								try{
									String responseMessage = in.readUTF();
									handleServerResponse(userInput, responseMessage, in);
								}
								catch(SocketTimeoutException e){
									//break;
								}
							}
							if(swatch.getTime()>1800){
								break;
							}
						}
						break;
					}//check ENTER ends.
				//keep open to receive asynchronous responses from server
				if(!hasSecureOption){
					if(in.available()>0){
						String responseMessage = in.readUTF();
						handleServerResponse(userInput, responseMessage, in);
					}
				}
				else{
					try{
						String responseMessage = in.readUTF();
						handleServerResponse(userInput, responseMessage, in);
					}
					catch(SocketTimeoutException e){
						//should NOT be any break here.
					}
				}
			}
				break;//???should here a break?
			default:
				//start timer, when over 1.3 second passed after the last JSON message was received, close socket.
				swatch.start();
				while(true){
					//when a JSON messages returned, reset and restart timer.
					if(!hasSecureOption){
						if(in.available()>0){
							String responseMessage = in.readUTF();
							handleServerResponse(userInput, responseMessage, in);
							swatch.reset();
							swatch.start();
						}
					}
					else{
						try{
							String responseMessage = in.readUTF();
							handleServerResponse(userInput, responseMessage, in);							
						}
						catch(SocketTimeoutException e){
							break;
						}
					}
					if(swatch.getTime()>1300){
						break;
					}
				}
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * read the user's input in argument, put the content into JSONObject, then return the JSONObeject. 
	 * @param args
	 * @return The JSONObeject
	 */
	public static JSONObject handleClientInput(String[] args){
		for(int i=0;i<args.length;i++){
			if (args[i].equals("-publish")||args[i].equals("-remove")||args[i].equals("-share")||
					args[i].equals("-query")||args[i].equals("-fetch")||args[i].equals("-exchange")||args[i].equals("-subscribe")){
				//extract the command type, so we still remember the command type when handling response.
				commandType = args[i];
				//insert "-command" at args[i] for better use options
		    		String[] argsWithCommand = new String[args.length+1];
		    		argsWithCommand[i] = "-command";
		    		System.arraycopy(args, 0, argsWithCommand, 0, i);
		    		System.arraycopy(args, i, argsWithCommand, i+1, args.length-i);
		    		args = new String [args.length+1];
		    		System.arraycopy(argsWithCommand, 0, args, 0, argsWithCommand.length);
		    		break;
			}
		}
		//if input contain "-debug", modify the args[] to better use options
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
		//if input contain "-secure", modify the args[] to better use options
		for(int i=0;i<args.length;i++){
			if(args[i].equals("-secure")){
			hasSecureOption=true;
	    		String[] argsWithSecure = new String[args.length+1];
	    		argsWithSecure[i+1] = "";
	    		System.arraycopy(args, 0, argsWithSecure, 0, i+1);
	    		System.arraycopy(args, i+1, argsWithSecure, i+2, args.length-1-i);
	    		args = new String [args.length+1];
	    		System.arraycopy(argsWithSecure, 0, args, 0, argsWithSecure.length);
	    		break;
			}
		}

		String command = "";
		String name = "";
	    String description = "";    
	    String uri = "";
	    String channel = "";
	    String owner = "";
	    String ezserver = null;// assigned to null according to instruction
	    String secret = "";
	    boolean relay = true;
	    String serversAll = "";
	    
	
	    Options options = new Options();
	    options.addOption("command",true,"input command"); 
	    options.addOption("name",true,"input name");
	    options.addOption("tags",true,"input tags");
	    options.addOption("description",true, "input description");
	    options.addOption("uri",true, "input uri");
	    options.addOption("channel",true, "input channel");
	    options.addOption("owner",true, "input owner");
	    options.addOption("ezserver",true, "input ezserver");
	    options.addOption("secret",true, "input secret");
	    options.addOption("relay",true, "input relay");
	    options.addOption("servers",true, "input servers");
	    options.addOption("debug",true, "input debug");
	    options.addOption("host",true, "input host");
	    options.addOption("port",true, "input port");
	    options.addOption("id",true, "input id");
	    options.addOption("secure",true, "input secure");
	    
	    CommandLineParser parser = new DefaultParser();
	    CommandLine cmd = null;
	
	    try{
	        cmd = parser.parse(options,args);      
	    } 
	    catch (org.apache.commons.cli.ParseException e) {
			e.printStackTrace();
		}
	    
	    JSONObject userinputTemp = new JSONObject();
	    JSONObject resource = new JSONObject();
 
	    if(cmd.hasOption("name")){
	       name = cmd.getOptionValue("name"); 
	   }
	    resource.put(ConstantEnum.CommandArgument.name.name(),name);
	   	    
	    if(cmd.hasOption("tags")){
	       String[] tags = cmd.getOptionValue("tags").split(",");
	       //Array<String> -> List -> JSONArray -> JSONObject
	       resource.put(ConstantEnum.CommandArgument.tags.name(), new JSONArray(Arrays.asList(tags)));
	   }
	    else {
	    	JSONArray emptyJSONArray = new JSONArray();
	    	resource.put(ConstantEnum.CommandArgument.tags.name(), emptyJSONArray);
	    }
	   	
	    if(cmd.hasOption("description")){
	       description = cmd.getOptionValue("description");
	   }
	    resource.put(ConstantEnum.CommandArgument.description.name(),description);	 
	    
	    if(cmd.hasOption("uri")){
	       uri = cmd.getOptionValue("uri");
	   }
	    resource.put(ConstantEnum.CommandArgument.uri.name(),uri);
	    
	    if(cmd.hasOption("channel")){
	       channel = cmd.getOptionValue("channel");
	   }
	    resource.put(ConstantEnum.CommandArgument.channel.name(),channel);	 
	       
	    if(cmd.hasOption("owner")){
	       owner = cmd.getOptionValue("owner");
	   }
	    resource.put(ConstantEnum.CommandArgument.owner.name(),owner);
	       
	    if(cmd.hasOption("ezserver")){
	       ezserver = cmd.getOptionValue("ezserver");
	   }
	    resource.put(ConstantEnum.CommandArgument.ezserver.name(),ezserver);
	    
	    if(cmd.hasOption("secret")){//it's only used in SHARE
	       secret = cmd.getOptionValue("secret");
	   }  
	       
	    if(cmd.hasOption("relay")){
	    		//convert the string the user input in -relay field to boolean.
	    		relay = Boolean.parseBoolean(cmd.getOptionValue("relay"));
	   }
	    
	    if(cmd.hasOption("host")){
			String hostipPattern = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
			String hostnamePattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
			if(Pattern.matches(hostnamePattern, cmd.getOptionValue("host"))||Pattern.matches(hostipPattern, cmd.getOptionValue("host"))){
				host = cmd.getOptionValue("host");
			}
			else System.out.println("invalid host");
	    }
	    
	    if(cmd.hasOption("port")){
	    	String portPattern = "^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$";
			if(Pattern.matches(portPattern, cmd.getOptionValue("port"))){
				port = Integer.parseInt(cmd.getOptionValue("port"));
			}
			else System.out.println("invalid port");
		}
	       
	    if(cmd.hasOption("servers")){
	       serversAll = cmd.getOptionValue("servers");
	       String[] serversArray = serversAll.split(",");
	       JSONArray serversJSONArray = new JSONArray();
	       for (int i=0; i<serversArray.length; i++){
	    	   		JSONObject temp = new JSONObject();
	    	   		String[] hostnameAndPort = serversArray[i].split(":");
	    	   		temp.put("hostname", hostnameAndPort[0]);
	    	   		temp.put("port", hostnameAndPort[1]);  
	    	   		serversJSONArray.put(temp);
	       }
	       userinputTemp.put(ConstantEnum.CommandArgument.serverList.name(),serversJSONArray);   
	   }
	   
	    if(cmd.hasOption("debug")){
		       hasDebugOption = true;
		   }   
	    
	    if(cmd.hasOption("id")){
		       id = cmd.getOptionValue("id");
		   }
	    
	    if(!cmd.hasOption("id")){
			Random random = new Random();
			id = RandomStringUtils.randomNumeric(5);
		   }
	    
	    if(cmd.hasOption("secure")){
		       hasSecureOption = true;
		   } 
	    
	    if(cmd.hasOption("command")){
	        command = cmd.getOptionValue("command");
	        switch (command){
	        case "-publish":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"PUBLISH");
	    						userinputTemp.put(ConstantEnum.CommandType.resource.name(),resource); 
	    						break;
	        case "-remove":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"REMOVE");
							userinputTemp.put(ConstantEnum.CommandType.resource.name(),resource); 
							break;
	        case "-share":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"SHARE"); 
							userinputTemp.put(ConstantEnum.CommandType.resource.name(),resource);
							userinputTemp.put(ConstantEnum.CommandArgument.secret.name(),secret);
							break;
	        case "-query":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"QUERY");
	        					userinputTemp.put(ConstantEnum.CommandArgument.relay.name(),relay); 
							userinputTemp.put(ConstantEnum.CommandArgument.resourceTemplate.name(),resource); 
							/*resource & rsourceTemplate are with different names but in same format, so
							1 JSONObject 'resource' is used as their format*/
							break;
	        case "-fetch":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"FETCH");
	        					userinputTemp.put(ConstantEnum.CommandArgument.resourceTemplate.name(),resource); 
	        					break;
	       						//serverArray has been put before, so here we just put command 	        					
	        case "-exchange":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"EXCHANGE");	
	       						break;
	        case "-subscribe":	userinputTemp.put(ConstantEnum.CommandType.command.name(),"SUBSCRIBE");
								userinputTemp.put(ConstantEnum.CommandArgument.relay.name(),relay); 
								userinputTemp.put(ConstantEnum.CommandArgument.id.name(),id); 
								userinputTemp.put(ConstantEnum.CommandArgument.resourceTemplate.name(),resource); 
	        default: break;	
	        }
	    }
	    else {
	    		//empty command field will be handled at the server
	    		command ="";
	    		userinputTemp.put(ConstantEnum.CommandType.command.name(), "");
	    }    
			return userinputTemp;
	}
	
	
	

	/**
	 * print out the response and debug information.
	 * @param input
	 */
	public static void handleServerResponse(JSONObject userInput, String input, DataInputStream in){

		try {
			//print part of the debug information
			JSONParser parser = new JSONParser();
			JSONObject serverResponse;		
			serverResponse = (JSONObject)parser.parse(input);
			if(hasDebugOption){
		       System.out.println("RECEIVED: "+serverResponse.toJSONString());
			}
			
			//print response in GSON format. 
			JsonParser jsonParser = new JsonParser();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonElement element = jsonParser.parse(input);
			String show = gson.toJson(element);
			
			//use the command type recorded when reading user's input
			switch (commandType){
			case "-publish":
			case "-remove":
			case "-share":
			case "-exchange":
			case "-query":
				break;
			case "-fetch":
				handleDownload(serverResponse,in);
				break;
			case "-subscribe":
				break;
			/*commandType remains "", and the pair {"command",""} was put into a JSONObject and sent to server.
				So here we just print out the error message returned from server. no need to handle empty command case*/
			default: 
				break;
			}

		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * This method handles the downloading part of the Fetch response.
	 * @param serverResponse
	 * @param in
	 */
	public static void handleDownload(JSONObject serverResponse, DataInputStream in) {
			if(serverResponse.containsKey("resourceSize")){
				try{
					// The file location to download to.
					
					/**regexp to extract file name from uri*/
					String uri = serverResponse.get("uri").toString();
					String regex = "(\\w+\\.\\w+)";
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(uri);
					
					while(matcher.find()){
						int start = matcher.start();
						int end = matcher.end();
						String fileName = uri.substring(start, end);
						System.out.println("client_files/"+fileName);
						String downloadFilePath = "client_files/"+fileName;
						
						// Create a RandomAccessFile to read and write the output file.
						RandomAccessFile downloadingFile = new RandomAccessFile(downloadFilePath, "rw");
						
						// Find out how much size is remaining to get from the server.
						long fileSizeRemaining = (Long) serverResponse.get("resourceSize");
						
						int chunkSize = setChunkSize(fileSizeRemaining);
						
						// Represents the receiving buffer
						byte[] receiveBuffer = new byte[chunkSize];
						
						// Variable used to read if there are remaining size left to read.
						int num;
						System.out.println("Downloading "+fileName+" of size "+fileSizeRemaining);
						while((num=in.read(receiveBuffer))>0){
							// Write the received bytes into the RandomAccessFile
							downloadingFile.write(Arrays.copyOf(receiveBuffer, num));
							
							// Reduce the file size left to read.
							fileSizeRemaining-=num;
							
							// Set the chunkSize again
							chunkSize = setChunkSize(fileSizeRemaining);
							receiveBuffer = new byte[chunkSize];
							
							// If it's done then break
							if(fileSizeRemaining==0){
								break;
						}
					}
					System.out.println("File received!");
					downloadingFile.close();
					}
					
					
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				
			}
			
		}
	}
	
	
	/**
	 * This method set the chunk size in each downloading. It's for handleDownload method.
	 * @param fileSizeRemaining
	 * @return the appropriate chunk size of next downloading.
	 */
	public static int setChunkSize(long fileSizeRemaining){
		// Determine the chunkSize
		int chunkSize=1024*1024;
		
		// If the file size remaining is less than the chunk size
		// then set the chunk size to be equal to the file size.
		if(fileSizeRemaining<chunkSize){
			chunkSize=(int) fileSizeRemaining;
		}
		return chunkSize;
	}
	
	

	
    /**
     * This method start an individual thread to listen for ENTER. 
     * When ENTER is pressed, it change enterDetected to true.
     */
    public static void startEnterListen(){
          Thread thread = new Thread(new Runnable(){
                @Override
                public void run() {
                            Scanner scanner = new Scanner(System.in);
                            scanner.nextLine();
                            enterDetected = true;
                }
           });
          thread.start();
    }
	
	
	
}

