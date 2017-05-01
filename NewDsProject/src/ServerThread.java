import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.management.Query;
import javax.xml.ws.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.simple.JSONArray;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.LongBinaryOperator;
import java.util.logging.Handler;

/**
 * This class mainly handle the JSON object received from the client.
 * It parse the message and extract command and its related fields in the message.
 *
 */
public class ServerThread extends Thread{
	
	Socket clientSocket;
	
	private HashMap<String, Resource> resources;
	
	private DataInputStream input;
	
	private DataOutputStream output;
	
	private String secret;
	
	public ServerSocket serverSocket;
	
	private ArrayList<String> serverList;
	
	public FetchResult fetchResult;
	
	public static boolean hasDebugOption;
	
	public static String hostName;
	
	public int interval;
	
	public ServerThread(Socket socket, HashMap<String, Resource> resources, String secret, ServerSocket serverSocket,
			ArrayList<String> serverList, boolean hasDebugOption, int interval, String ServerHostName){
		//try {
			this.clientSocket = socket;
			this.resources = resources;	
			this.secret = secret;
			try {
				this.output = new DataOutputStream(clientSocket.getOutputStream());
				this.input = new DataInputStream(clientSocket.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.serverSocket = serverSocket;
			this.serverList = serverList;
			this.hasDebugOption = hasDebugOption;
			this.interval = interval;
			this.hostName = ServerHostName;
			
			/**set interval limit*//*
			new Timer().scheduleAtFixedRate(new TimerTask() {
				
				@Override
				public void run() {
					try {
						String inputMessage = input.readUTF();
						handleCommand(inputMessage);
						
						
					} catch (Exception e) {
						// TODO: handle exception
					}
					
				}
			}, 0, interval);
			
		} catch (IOException e) {
			if(clientSocket!=null){
				try {
					clientSocket.close();
				} catch (IOException e2) {
					
				}
			}
		}*/
	}
	
	
	
	@Override
	public void run() {
		try {
			String inputMessage = input.readUTF();
			handleCommand(inputMessage);
			
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public static String[] handleTags(String str){
		
		/*String removeQuote = str.substring(1, str.length()-1);*/
		String removeQuote = str.replaceAll("\\[|\\]", "");
		String finalStr = removeQuote.replaceAll("\"", "");
		return finalStr.split(",");
	}
	
	/**
	 * This method recognize the command type and extract information in fields accordingly.
	 * @param string the JSON message received on socket from client
	 */
	public synchronized void handleCommand (String string){
		JSONParser parser = new JSONParser();
		JSONObject jsonObject;
		JSONObject sendResponse;
		JSONObject localResponse;
		JsonArray jsonArray;
		QueryData queryData;
		try {
			jsonObject = (JSONObject) parser.parse(string);
			
			//print debug information
			if(hasDebugOption){
			       System.out.println("RECEIVED: "+jsonObject.toJSONString());
				}
			
			/*ConstantEnum.CommandType command  = ConstantEnum.CommandType.valueOf((String)jsonObject.get("command"));*/
			String command = (String) jsonObject.get(ConstantEnum.CommandType.command.name());
						switch (command) {
			case "DEBUG":
				break;
			case "PUBLISH":
				/**取出嵌套在jsonObject中的resource字段（同样也是jsonObjecy）*/
				JSONObject resource_publish = (JSONObject) jsonObject.get("resource");
				//System.out.println(resource_publish.toJSONString());
				
				String[] tags = handleTags(resource_publish.get(ConstantEnum.CommandArgument.tags.name()).toString());
				ArrayList<String> tag = tagTolist(tags);
				String name = resource_publish.get(ConstantEnum.CommandArgument.name.name()).toString();
				//System.out.println(name.length()+" "+name);
				String description = resource_publish.get(ConstantEnum.CommandArgument.description.name()).toString();
				String uri = resource_publish.get(ConstantEnum.CommandArgument.uri.name()).toString();
				String channel = resource_publish.get(ConstantEnum.CommandArgument.channel.name()).toString();
				String owner = resource_publish.get(ConstantEnum.CommandArgument.owner.name()).toString();
	
				sendResponse = ServerHandler.handlingPublish(name,tags,description,uri,channel,owner,this.resources);
				sendMessage(sendResponse);
				break;
			case "REMOVE":
				JSONObject resource_remove = (JSONObject) jsonObject.get("resource");
				
				String [] tags_remove = handleTags(resource_remove.get(ConstantEnum.CommandArgument.tags.name()).toString());
				String name_remove = resource_remove.get(ConstantEnum.CommandArgument.name.name()).toString();
				
				String description_remove = resource_remove.get(ConstantEnum.CommandArgument.description.name()).toString();
			
				String uri_remove = resource_remove.get(ConstantEnum.CommandArgument.uri.name()).toString();
				
				String channel_remove = resource_remove.get(ConstantEnum.CommandArgument.channel.name()).toString();
			
				String owner_remove = resource_remove.get(ConstantEnum.CommandArgument.owner.name()).toString();
				//EZserver is not here!
			
				/**get response with the remove command*/
				/*sendResponse = ServerHandler.handlingRemove(new Resource(name_remove, tag_remove, description_remove, 
						uri_remove, channel_remove, owner_remove),this.resources);*/
				sendResponse = ServerHandler.handlingRemove(name_remove,tags_remove,description_remove,uri_remove,channel_remove,owner_remove,this.resources);		
				sendMessage(sendResponse);
				break;
			case "SHARE":
				JSONObject resource_share = (JSONObject) jsonObject.get("resource");
				String [] tags_share = handleTags(resource_share.get(ConstantEnum.CommandArgument.tags.name()).toString());
				String name_share = resource_share.get(ConstantEnum.CommandArgument.name.name()).toString();
				String description_share = resource_share.get(ConstantEnum.CommandArgument.description.name()).toString();
				String uri_share = resource_share.get(ConstantEnum.CommandArgument.uri.name()).toString();
				String channel_share = resource_share.get(ConstantEnum.CommandArgument.channel.name()).toString();
				String owner_share = resource_share.get(ConstantEnum.CommandArgument.owner.name()).toString();
				String secret_share = jsonObject.get(ConstantEnum.CommandArgument.secret.name()).toString();
				
				//EZserver is not here!
				
				/**get response with the share command*/					
				sendResponse = ServerHandler.HandlingShare(name_share, tags_share, description_share, uri_share, 
						channel_share, owner_share, secret_share,this.secret,this.resources);
				sendMessage(sendResponse);
				break;
			case "FETCH":
				JSONObject fecthTemplate = (JSONObject) jsonObject.get("resourceTemplate");
				
				String [] tags_fetch = handleTags(fecthTemplate.get(ConstantEnum.CommandArgument.tags.name()).toString());
				String name_fetch = (String) fecthTemplate.get(ConstantEnum.CommandArgument.name.name());
				String description_fetch = (String) fecthTemplate.get(ConstantEnum.CommandArgument.description.name());
				String uri_fetch = (String) fecthTemplate.get(ConstantEnum.CommandArgument.uri.name());
				String channel_fetch = (String) fecthTemplate.get(ConstantEnum.CommandArgument.channel.name());
				String owner_fetch = (String) fecthTemplate.get(ConstantEnum.CommandArgument.owner.name());
				
				handlingFetch(name_fetch, tags_fetch, description_fetch, uri_fetch, channel_fetch, owner_fetch, resources, serverSocket, hostName);
				break;
			case "QUERY":
				
				JSONObject template_resource = (JSONObject)jsonObject.get("resourceTemplate");
				JSONArray debugMsg = new JSONArray();
				boolean relay1;
				
				String [] tags_query = handleTags(template_resource.get(ConstantEnum.CommandArgument.tags.name()).toString());
				String name_query = template_resource.get(ConstantEnum.CommandArgument.name.name()).toString();
				String description_query = template_resource.get(ConstantEnum.CommandArgument.description.name()).toString();
				String uri_query = template_resource.get(ConstantEnum.CommandArgument.uri.name()).toString();
				String channel_query = template_resource.get(ConstantEnum.CommandArgument.channel.name()).toString();
				String owner_query = template_resource.get(ConstantEnum.CommandArgument.owner.name()).toString();
				String relay = jsonObject.get(ConstantEnum.CommandArgument.relay.name()).toString();
				if(relay.equals("")){
					relay1 = true;
				}else{
					relay1 = Boolean.parseBoolean(relay);
				}
				
				/**just query local resources*/
				if(relay1==false){
					QueryReturn queryReturn = ServerHandler.handlingQuery(name_query, tags_query, description_query,
							uri_query, channel_query, owner_query,relay1,this.resources, this.serverSocket,this.hostName);
					if (queryReturn.hasMatch==false) {
						sendMessage(queryReturn.reponseMessage);
					}else{
						try {
							int length = queryReturn.returnList.size();
							for(int i=0;i<length;i++){
								output.writeUTF(queryReturn.returnList.get(i).toString());
								debugMsg.add(queryReturn.returnList.get(i));
							}
							
							/*output.writeUTF(queryReturn.returnArray.toString());*/
							output.flush();
							if(hasDebugOption){
								System.out.println("SENT: "+debugMsg.toJSONString());
							}
							/*System.out.println(Thread.currentThread().getName()+": has matched,sending response message!");*/
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.err.println(Thread.currentThread().getName() + ":Error while sending");
						}
					}
					
					/**relay field is true, handle both local query and query with other server in the server list*/
				}else{
					QueryReturn localReturn = ServerHandler.handlingQuery(name_query, tags_query, description_query, uri_query, channel_query, owner_query,relay1,this.resources, this.serverSocket,this.hostName);
					
					
					queryData = ServerHandler.handlingQueryWithRelay(string, this.resources, this.serverSocket, this.serverList,this.hasDebugOption);
					handleRelay(queryData, localReturn);
				}
				
				/*QueryReturn queryReturn = ServerHandler.handlingQuery(name_query, tags_query, description_query, uri_query, channel_query, owner_query,relay1,this.resources, this.serverSocket);*/
				break;
			case "EXCHANGE":
				JSONArray serverListJSONArray = (JSONArray) jsonObject.get("serverList");// need to deal with "serverList" missing	!
				ArrayList<String> serverList_exchange = new ArrayList<>();
				ArrayList<String> hostnameList_exchange = new ArrayList<>();
				ArrayList<String> portList_exchange = new ArrayList<>();
				for(/*JSONObject serversJSONObject:serverListJSONArray*/int i=0; i<serverListJSONArray.size(); i++){
					JSONObject serverJSONObject = (JSONObject)serverListJSONArray.get(i);
					String hostname = serverJSONObject.get("hostname").toString();
					String port = serverJSONObject.get("port").toString();
					String hostnameAndPort = hostname+":"+port;
					hostnameList_exchange.add(hostname);
					portList_exchange.add(port);
					serverList_exchange.add(hostnameAndPort);
				}
				sendResponse = ServerHandler.handlingExchange(serverList, serverList_exchange, hostnameList_exchange, portList_exchange);
				sendMessage(sendResponse);
				break;
			default:				
				break;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method handle both local query outcomes and other servers' query outcomes,
	 * then merge local and both outcomes and send back to the client
	 * @param QueryData
	 * @param QueryReturn
	 */
	public synchronized void handleRelay(QueryData otherResponse,QueryReturn localReturn){
		ArrayList<JSONObject> mergeList = new ArrayList<>();
		JSONArray debugMsg = new JSONArray();
		
		//other servers no response or no match, return local query outcome
		if(otherResponse == null||otherResponse.hasMatch==false){
			if (localReturn.hasMatch==false) {
				sendMessage(localReturn.reponseMessage);
			}else{
				try {
					int length = localReturn.returnList.size();
					
					for(int i=0;i<length;i++){
						output.writeUTF(localReturn.returnList.get(i).toString());
						
						debugMsg.add(localReturn.returnList.get(i));					
					}
					
					/*output.writeUTF(queryReturn.returnArray.toString());*/
					output.flush();
					if (hasDebugOption) {
						System.out.println("SENT: "+debugMsg.toJSONString());
					}
					/*System.out.println(Thread.currentThread().getName()+": has matched,sending response message!");*/
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println(Thread.currentThread().getName() + ":Error while sending");
				}
			}
			
		
		}else{
			//local query error, return other server response
			if(localReturn.hasMatch==false){
				int length = otherResponse.outcome.size();
				try {
					JSONObject successMsg = new JSONObject();
					successMsg.put("success", "success");
					
					output.writeUTF(successMsg.toJSONString());
					debugMsg.add(successMsg);
					
					for(int i=0;i<length;i++){
						output.writeUTF(otherResponse.outcome.get(i).toJSONString());
						output.flush();
						debugMsg.add(otherResponse.outcome.get(i));
					}
					
					JSONObject returnSize = new JSONObject();
					returnSize.put("resultSize", length);
					debugMsg.add(returnSize);
					output.writeUTF(returnSize.toJSONString());
					if (hasDebugOption) {
						System.out.println("SENT: "+debugMsg.toJSONString());
					}
					
				} catch (Exception e) {
					
				}
					
				//local and other both have match
			}else{
				int length = otherResponse.outcome.size();
				
				JSONObject successMsg = new JSONObject();
				successMsg.put("response", "success");
				try {
					output.writeUTF(successMsg.toJSONString());
					
					debugMsg.add(successMsg);
					
					for(int i=0;i<length;i++){
						output.writeUTF(otherResponse.outcome.get(i).toJSONString());
						output.flush();
						debugMsg.add(otherResponse.outcome.get(i));
					}
					
					int length2 = localReturn.returnList.size();
					for(int i=1;i<length2-1;i++){
						output.writeUTF(localReturn.returnList.get(i).toString());
						output.flush();		
						debugMsg.add(localReturn.returnList.get(i));
						
					}
					
					int totalLength = length+length2-2;
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("resultSize", totalLength);
					output.writeUTF(jsonObject.toJSONString());
					output.flush();
					debugMsg.add(jsonObject);
					if(hasDebugOption){
						System.out.println("SENT: "+debugMsg.toJSONString());
					}
					
				} catch (Exception e) {
					// TODO: handle exception
				}
				
			}
		}
		
	}
	
	/**
	 * This method sends response message from the server to client
	 * @param message
	 */
	public synchronized void sendMessage(JSONObject message){
		try {
			
			output.writeUTF(message.toJSONString());
			output.flush();
			if(hasDebugOption){
			       System.out.println("SENT: "+message.toJSONString());
				}
			System.out.println(Thread.currentThread().getName()+":sending response message!");
			
		} catch (IOException e) {
			System.err.println(Thread.currentThread().getName() + ":Error while sending");
		}
	}
	
	public static  ArrayList<String> tagTolist (String[] str){
		ArrayList<String> list = new ArrayList<String>();
		for(String string: str){
			list.add(string);
			}
		return list;
	}
	
	/**
	 * This method handles fetch command
	 * @param name
	 * @param tags
	 * @param description
	 * @param uri
	 * @param channel
	 * @param owner
	 * @param resources
	 * @param serverSocket
	 * @param hostName
	 */
	public synchronized void handlingFetch(String name,String[] tags,
			String description, String uri,String channel, 
			String owner,HashMap<String, Resource> resources, ServerSocket serverSocket, String hostName){
		String errorMessage;
		String response;
		JSONObject serverResponse = new JSONObject();
		JSONArray debugMsg = new JSONArray();
		
		/**a fetchResult store the server response and file data if fetch template is matched*/
		FetchResult fetchResult = new FetchResult();
		
		/**Regexp for filePath*/
		/*String filePathPattern = "^[a-zA-Z*]:?([\\\\/]?|([\\\\/]([^\\\\/:\"<>|]+))*)[\\\\/]?$|^\\\\\\\\(([^\\\\/:\"<>|]+)[\\\\/]?)+$";*/
		/*String filePathPattern = "(\\w+\\/)|(\\w+\\\\)";*/
		/*String filePathPattern = "(\\w+\\/\\w+.\\w+)|(\\w+\\\\\\w+.\\w+)";*/
		String filePathPattern = "((\\w+\\/)+)+(\\w+.\\w+)";
		/**Regexp for invalid resource contains whitespace or /o */
		String invalidString = "(^\\s.+\\s$)|((\\\\0)+)";
		
		/**invalid resource contains whitespace or \o */
		boolean invalidTag = false;
		for(String str: tags){
			if(Pattern.matches(invalidString, str)){
				invalidTag = true;
			}
		}
		
		boolean invalidResourceValue = Pattern.matches(invalidString, name)||Pattern.matches(invalidString, channel)||
				Pattern.matches(invalidString, description)||Pattern.matches(invalidString, uri)||
				Pattern.matches(invalidString, owner)||invalidTag;
		do {
			if(invalidResourceValue || owner.equals("*")){
				errorMessage = "invalid resourceTemplate";
				response = "error";
				serverResponse.put(ConstantEnum.CommandType.response.name(),response);
				serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
				/*try {
					this.output.writeUTF(serverResponse.toJSONString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				sendMessage(serverResponse);
			}else{
				//|| !Pattern.matches(filePathPattern,uri)
				if(uri.equals("") ){
					errorMessage = "missing resourceTemplate";
					response = "error";
					serverResponse.put(ConstantEnum.CommandType.response.name(),response);
					serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					/*try {
						this.output.writeUTF(serverResponse.toJSONString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}*/
					sendMessage(serverResponse);
				}else{
					boolean hasMacthResource = false;
					/*hasMacthResource = resources.get("uri").equals(uri)&&resources.get("channel").equals(channel);*/
					hasMacthResource = resources.containsKey(uri)&&resources.get(uri).channel.equals(channel);
					
					if (hasMacthResource) {
						String fileName = resources.get(uri).name;
						/*File file = new File(uri+fileName);*/
						//String string = uri.substring(8, uri.length());
						File file = new File("");
						try {
							file = new File(new URI(uri).getPath());
						} catch (URISyntaxException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						//System.out.println(string+"   "+file.exists());
						
						
						
						if(file.exists()){
							/*System.out.println("file exists!");*/
							JSONObject matchResource = new JSONObject();
							JSONArray jsonArray = new JSONArray();		
							
							response = "success";
							serverResponse.put(ConstantEnum.CommandType.response.name(), response);
							
							/*jsonArray.add(serverResponse);*/
							
							matchResource.put("name", resources.get(uri).name);
							JSONArray tagsArray = new JSONArray();
							for (String tag: resources.get(uri).tag){
								tagsArray.add(tag);
							}
							matchResource.put(ConstantEnum.CommandArgument.tags.name(), tagsArray);
							matchResource.put(ConstantEnum.CommandArgument.description.name(), resources.get(uri).description);
							matchResource.put(ConstantEnum.CommandArgument.uri.name(), resources.get(uri).URI);
							matchResource.put(ConstantEnum.CommandArgument.channel.name(),resources.get(uri).channel);
							
							/**if owner not "", replace it with * */
							if(resources.get(uri).owner.equals("")){
								matchResource.put(ConstantEnum.CommandArgument.owner.name(), resources.get(uri).owner);
							}else{
								matchResource.put(ConstantEnum.CommandArgument.owner.name(), "*");
							}
							
							Integer ezport = serverSocket.getLocalPort();
							String ezserver = hostName+":"+ezport.toString();
							matchResource.put(ConstantEnum.CommandArgument.ezserver.name(), ezserver);
							matchResource.put("resourceSize", file.length());
							
							ArrayList<JSONObject> map = new ArrayList<>();
							/*jsonArray.add(matchResource);*/
							map.add(serverResponse);
							map.add(matchResource);
							
							try {
								
								for(JSONObject object:map){
									output.writeUTF(object.toJSONString());
									output.flush();
									debugMsg.add(object);
									//debug information
								}
								if(hasDebugOption){
								       System.out.print("SENT: "+debugMsg.toJSONString());
									}
								
								// start sending file
								RandomAccessFile byteFile = new RandomAccessFile(file, "r");
								byte[] sendingBuffer = new byte[1024*1024];
								int num;
								while((num = byteFile.read(sendingBuffer))>0){
									System.out.println(num);
									output.write(Arrays.copyOf(sendingBuffer, num));
								}
								byteFile.close();
								
							} catch (IOException e) {
								e.printStackTrace();
							}		
						}else{
							System.out.println("file do not exists");
							errorMessage = "invalid resourceTemplate";
							response = "error";
							serverResponse.put(ConstantEnum.CommandType.response.name(),response);
							serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
						
							sendMessage(serverResponse);
						}
						
					}else{
						System.out.println("file do not exists");
						errorMessage = "invalid resourceTemplate";
						response = "error";
						serverResponse.put(ConstantEnum.CommandType.response.name(),response);
						serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					
						sendMessage(serverResponse);
					}
					
				}
			}
		
		} while (serverResponse==null);

	}
	

	
}
