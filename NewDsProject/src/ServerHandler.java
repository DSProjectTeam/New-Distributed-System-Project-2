import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

import javax.swing.OverlayLayout;

import org.apache.commons.lang3.time.StopWatch;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This class mainly create the message to be returned to client, according to clients' requests.
 *
 */
public class ServerHandler {
	
	public Queue<String> clientQueue;
	
	/**
	 * This method creates the JSON object to be returned to client, in response to publish command.
	 * @param name
	 * @param tags
	 * @param description
	 * @param uri
	 * @param channel
	 * @param owner
	 * @param resources
	 * @return the JSON object to be returned to client
	 */
	public synchronized static JSONObject handlingPublish(String name,String[] tags,
			String description, String uri,String channel, 
			String owner,HashMap<String, Resource> resources){
		String errorMessage;
		String response;
		Boolean validUri;
			
		/**reponse send back to the client*/
		JSONObject serverResponse = new JSONObject();
		
		/**Regexp for filePath*/
		/*String filePathPattern = "(^[A-Z|a-z]:\\/[^*|\"<>?\\n]*)|(\\/\\/.*?\\/.*)";*/
		/*String filePathPattern = "(\\w+\\/\\w+.\\w+)|(\\w+\\\\\\w+.\\w+)";*/
		String filePathPattern = "((\\w+\\/)+)+(\\w+.\\w+)";
		/**Regexp for invalid resource contains whitespace or /o */
		String invalidString = "(^\\s.+\\s$)|((\\\\0)+)";
		/**invalid resource contains whitespace or /o */
		boolean invalidTag = false;
		for(String str: tags){
			if(Pattern.matches(invalidString, str)){
				invalidTag = true;
			}
		}
		boolean invalidResourceValue = Pattern.matches(invalidString, name)||Pattern.matches(invalidString, channel)||
				Pattern.matches(invalidString, description)||Pattern.matches(invalidString, uri)||
				Pattern.matches(invalidString, owner)||invalidTag;
		do{
			if (uri.equals("")) {
				errorMessage = "missing resource";
				response = "error";
				serverResponse.put(ConstantEnum.CommandType.response.name(),response);
				serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
			}else{
				/**Use URI class method to authenticate input URI*/
				try {
					URI inputUri = new URI(uri);
					if (inputUri.isAbsolute()&&!inputUri.getScheme().equals("file")) {
						validUri = true;
					}else{
						validUri = false;
					}
				} catch (URISyntaxException e) {
					validUri = false;
					System.out.println("invalid uri");
				}
				/**resource field not given or uri is not file scheme*/
				if(invalidResourceValue|| owner.equals("*")||!validUri){
					errorMessage = "invalid resource";
					response = "error";
					System.out.println(invalidResourceValue+"  "+owner.equals("*")+"  "+validUri);
					serverResponse.put(ConstantEnum.CommandType.response.name(),response);
					serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					
				}else{
					if(resources.containsKey(uri)){
						/**same URI, same channel,different owner or owner contains * */
						if(resources.get(uri).channel.equals(channel) &&!resources.get(uri).owner.equals(owner)){
							errorMessage = "cannot publish resource";
							response="error";
							serverResponse.put(ConstantEnum.CommandType.response.name(),response);
							serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
						}else{
							/**same primary key*/
							
								resources.remove(uri);
								Resource resource = new Resource(name, tags, description, uri, channel, owner);
								resources.put(resource.URI, resource);
								response = "success";
								serverResponse.put(ConstantEnum.CommandType.response.name(),response);	
						}
					}else{
						/**valid URI*/
						Resource resource = new Resource(name, tags, description, uri, channel, owner);
						resources.put(uri,resource);
						response = "success";
						serverResponse.put(ConstantEnum.CommandType.response.name(),response);				
					}
				}
			}
		}while(serverResponse==null);
		
		
		return serverResponse;
	}
	
	/**
	 * This method creates the JSON object to be returned to client, in response to Remove command.
	 * @param name
	 * @param tags
	 * @param description
	 * @param uri
	 * @param channel
	 * @param owner
	 * @param resources
	 * @return the JSON object to be returned to client
	 */
	public synchronized static JSONObject handlingRemove (String name,String[] tags,
			String description, String uri,String channel, 
			String owner,HashMap<String, Resource> resources){
		String errorMessage;
		String response;
		Boolean success = false;
		/**reponse send back to the client*/
		JSONObject serverResponse = new JSONObject();
		/**Regexp for filePath*/
		String filePathPattern = "((\\w+\\/)+)+(\\w+.\\w+)";
		/**Regexp for invalid resource contains whitespace or /o */
		String invalidString = "(^\\s.+\\s$)|((\\\\0)+)";
		
		/**invalid resource contains whitespace or /o */
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
			if (invalidResourceValue || owner.equals("*")) {
				errorMessage = "invalid resource";
				response = "error";
				serverResponse.put(ConstantEnum.CommandType.response.name(),response);
				serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
			}else{
				/**resource field not given or uri is not file scheme*/
				//if(uri.equals("") || Pattern.matches(filePathPattern, uri))
				if(uri.equals("")){
					errorMessage = "missing resource";
					response = "error";
					serverResponse.put(ConstantEnum.CommandType.response.name(),response);
					serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					
				}else{
					
					/**successful remove*/
					if (resources.containsKey(uri)&&resources.get(uri).owner.equals(owner)&&
							resources.get(uri).channel.equals(channel))
					/*if (resources.containsKey(uri))*/ {
						response = "success";
						resources.remove(uri);
						serverResponse.put(ConstantEnum.CommandType.response.name(), response);
					}else{
						
						/**resource did not exist*/
						response = "error";
						errorMessage = "cannot remove resource";
						serverResponse.put(ConstantEnum.CommandType.response.name(), response);
						serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					}	
				}
			}
		} while (serverResponse==null);
		
		return serverResponse;	
	}
	
	/**
	 * This method creates the JSON object to be returned to client, in response to Share command.
	 * @param name
	 * @param tags
	 * @param description
	 * @param uri
	 * @param channel
	 * @param owner
	 * @param ClientSecret
	 * @param ServerSecret
	 * @param resources
	 * @return the JSON object to be returned to client
	 */
	public synchronized static JSONObject HandlingShare (String name,String[] tags,
			String description, String uri,String channel, 
			String owner, String ClientSecret, String ServerSecret, HashMap<String, Resource> resources){
		String errorMessage;
		String response;
		Boolean success = false;	
		/**reponse send back to the client*/
		JSONObject serverResponse = new JSONObject();
		boolean validUri;
		/**Regexp for filePath*/
		/*String filePathPattern = "^[a-zA-Z*]:?([\\\\/]?|([\\\\/]([^\\\\/:\"<>|]+))*)[\\\\/]?$|^\\\\\\\\(([^\\\\/:\"<>|]+)[\\\\/]?)+$";*/
		/*String filePathPattern = "(^[A-Z|a-z]:\\/[^*|\"<>?\\n]*)|(\\/\\/.*?\\/.*)";*/
		/*String filePathPattern = "(\\w+\\/)|(\\w+\\\\)";*/
		/*String filePathPattern = "(\\w+\\/\\w+.\\w+)|(\\w+\\\\\\w+.\\w+)";*/
		String filePathPattern = "((\\w+\\/)+)+(\\w+.\\w+)";
		/**Regexp for invalid resource contains whitespace or /o */
		String invalidString = "(^\\s.+\\s$)|((\\\\0)+)";
		
		/**invalid resource contains whitespace or /o */
		boolean invalidTag = false;
		for(String str: tags){
			if(Pattern.matches(invalidString, str)){
				invalidTag = true;
			}
		}
		boolean invalidResourceValue = Pattern.matches(invalidString, name)||Pattern.matches(invalidString, channel)||
				Pattern.matches(invalidString, description)||Pattern.matches(invalidString, uri)||
				Pattern.matches(invalidString, owner)||invalidTag;
		
		/** resource or secret field was not given or not of the correct type*/
		do {
			if(ClientSecret.equals("") || uri.equals("")){
				response = "error";
				errorMessage = "missing resource and\\/or secret";
				serverResponse.put(ConstantEnum.CommandType.response.name(), response);
				serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);

			}else{
				if(!ClientSecret.equals(ServerSecret)){
					System.out.println(ClientSecret+" "+ServerSecret);
					/** secret was incorrect*/
					response = "error";
					errorMessage = "incorrect secret";
					serverResponse.put(ConstantEnum.CommandType.response.name(), response);
					serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
				}else{
					try {
						URI inputUri = new URI(uri);
						if (inputUri.isAbsolute()&&inputUri.getScheme().equals("file")) {
							validUri = true;
						}else{
							validUri = false;
						}
					} catch (URISyntaxException e) {
						validUri = false;
					}
					
					if (invalidResourceValue||owner.equals("*")||!validUri) {
						
						/** resource contained incorrect information that could not be recovered from*/
						errorMessage = "invalid resource";
						response = "error";
						serverResponse.put(ConstantEnum.CommandType.response.name(),response);
						serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					}else{
						if(resources.containsKey(uri)){
							/**same URI, same channel,different owner */
							if(!resources.get(uri).owner.equals(owner)
									&&resources.get(uri).channel.equals(channel)){
								errorMessage = "cannot publish resource";
								response="error";
								serverResponse.put(ConstantEnum.CommandType.response.name(),response);
								serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
							}else{
								/**same primary key*/
								resources.remove(uri);
								
								
								Resource resource = new Resource(name, tags, description, uri, channel, owner);
								resources.put(resource.URI, resource);
								response = "success";
								success = true;
								serverResponse.put(ConstantEnum.CommandType.response.name(),response);
								
								//right here share mechanism is not implement yet.!!!
							}
						}else{
							/**valid URI*/
							
							Resource resource = new Resource(name, tags, description, uri, channel, owner);
							resources.put(resource.URI,resource);
					
							//need a share mechanism!!
							response = "success";
							success= true;
							serverResponse.put(ConstantEnum.CommandType.response.name(),response);				
						}
					}
				}	
			}	
		} while (serverResponse==null);
		
	return serverResponse;	}
	
	/**
	 * This method creates the JSON objects to be returned to client, in response to Query command.
	 * @param name_query
	 * @param tags_query
	 * @param description_query
	 * @param uri_query
	 * @param channel_query
	 * @param owner_query
	 * @param relay
	 * @param resources
	 * @param serverSocket
	 * @param hostName
	 * @return the JSON object to be returned to client
	 */
	public synchronized static QueryReturn handlingQuery(String name_query,String[] tags_query,
			String description_query, String uri_query,String channel_query, 
			String owner_query, boolean relay,HashMap<String, Resource> resources, ServerSocket serverSocket,String hostName){
		/**用来存放满足template的resource*/
		ArrayList<Resource> matchResourceSet = new ArrayList<Resource>();
		String errorMessage;
		String response;
		org.json.JSONArray returnArray = new org.json.JSONArray();
		JSONArray returnArray1 = new JSONArray();	
		ArrayList<JSONObject> returnList = new ArrayList<>();
		QueryReturn queryReturn;
		JSONObject serverResponse = new JSONObject();
		

		/**Regexp for filePath*/
		String filePathPattern = "((\\w+\\/)+)+(\\w+.\\w+)";
		/**Regexp for invalid resource contains whitespace or /o */
		String invalidString = "(^\\s.+\\s$)|((\\\\0)+)";
		
		
		/**invalid resource contains whitespace or \o */
		boolean invalidTag = false;
		for(String str: tags_query){
			if(Pattern.matches(invalidString, str)){
				invalidTag = true;
			}
		}
		
		boolean invalidResourceValue = Pattern.matches(invalidString, name_query)||Pattern.matches(invalidString, channel_query)||
				Pattern.matches(invalidString, description_query)||Pattern.matches(invalidString, uri_query)||
				Pattern.matches(invalidString, owner_query)||invalidTag;
		//System.out.println("querying");
		
		boolean hasMacthResource = false;
		do{
			if(invalidResourceValue||owner_query.equals("*")){
				errorMessage = "invalid resourceTemplate";
				response = "error";
				serverResponse.put(ConstantEnum.CommandType.response.name(),response);
				serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
				queryReturn = new QueryReturn(serverResponse);
			}else{
				
						//**tagIncluded等于true如果所有template标签包含在候选资源的tags中*//*
						
					
					/** for query like -query with no parameter*/
					if(channel_query.equals("")&& owner_query.equals("") && uri_query.equals("") && name_query.equals("")
							&& description_query.equals("")&&tags_query[0].equals("")){
						ArrayList<Resource> allResource = new ArrayList<Resource>();
						if(!resources.isEmpty()){
							JSONObject returnSize = new JSONObject();
							for(Map.Entry<String, Resource> x:resources.entrySet()){
								allResource.add(x.getValue());
							}
							
							serverResponse.put(ConstantEnum.CommandType.response, "success");
							/*returnArray.put(serverResponse);*/
							
							/*returnArray1.add(serverResponse);*/
							returnList.add(serverResponse);
							for(Resource resourceTemp: allResource){
								
								JSONObject MatchResouce = new JSONObject();
								MatchResouce.put(ConstantEnum.CommandArgument.name.name(), resourceTemp.name);
								JSONArray tagsArray = new JSONArray();
								for (String tag: resourceTemp.tag){
									tagsArray.add(tag);
								}
								MatchResouce.put(ConstantEnum.CommandArgument.tags.name(), tagsArray);
								MatchResouce.put(ConstantEnum.CommandArgument.description.name(), resourceTemp.description);
								MatchResouce.put(ConstantEnum.CommandArgument.uri.name(), resourceTemp.URI);
								MatchResouce.put(ConstantEnum.CommandArgument.channel.name(), resourceTemp.channel);
								
								/**if owner not "", replace it with * */
								if(resourceTemp.owner.equals("")){
									MatchResouce.put(ConstantEnum.CommandArgument.owner.name(), resourceTemp.owner);
								}else{
									MatchResouce.put(ConstantEnum.CommandArgument.owner.name(), "*");
								}
								
								Integer ezport = serverSocket.getLocalPort();
								/*String ezserver = serverSocket.getInetAddress().toString()+":"+ezport.toString();*/
								String ezserver = hostName+":"+ezport.toString();
								MatchResouce.put(ConstantEnum.CommandArgument.ezserver.name(), ezserver);
								
								/*returnArray1.add(MatchResouce);*/
								returnList.add(MatchResouce);
								
							}
							returnSize.put(ConstantEnum.CommandType.resultSize, allResource.size());
							
							
							/*returnArray1.add(returnSize);*/		
							returnList.add(returnSize);							
							
							/*queryReturn = new QueryReturn(returnArray1);*/
							queryReturn = new QueryReturn(returnList); 
							hasMacthResource = true;
						}else{
							errorMessage = "no resource in store";
							response = "error";
							serverResponse.put(ConstantEnum.CommandType.response.name(),response);
							serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
							queryReturn = new QueryReturn(serverResponse);
						}
						
					}else{
						for(Resource resource : resources.values()){
							
							/**owner or URI not ""*/ 
							//&& tagIncluded
							//channel is working
							//caution: tags name
							//String a = (b ==null) ? true : false ;
						boolean channelMatch = (channel_query.equals(""))? true : channel_query.equals(resource.channel);
						boolean ownerMatch = (owner_query.equals("")) ? true: owner_query.equals(resource.owner);
						boolean uriMatch = (uri_query.equals("")) ?  true : uri_query.equals(resource.URI) ;
						
						boolean tagIncluded;
						
						
						if(tags_query[0].equals("")){
							tagIncluded = true;
						}else{
							if (!resource.tag[0].equals("")) {
								int tagLength = tags_query.length;
								int aaa = resource.tag.length;
								int tagCount=0;
								for(int i = 0; i<tagLength; i++){
									for(int j = 0; j<aaa; j++){
										if(tags_query[i].equals(resource.tag[j])){									
											tagCount++;
										}
									}
								}
								if (tagCount==tagLength) {
									tagIncluded = true;
								} else {
									tagIncluded = false;
								}
							}else{
								tagIncluded = true;
							}
						}
						
						if((channelMatch&& tagIncluded&& ownerMatch && uriMatch && ( (!name_query.equals("") && resource.name.contains(name_query))|| 
								(!description_query.equals("") && resource.description.contains(channel_query) )|| 
								(name_query.equals("")&&description_query.equals(""))))){
							System.out.println("match");
							
							/**put the match results into the MatchResourceSet*/
							matchResourceSet.add(resource);
						}
						
						}
						if(!matchResourceSet.isEmpty()){
							Integer size =1;							
							response = "success";							
							serverResponse.put(ConstantEnum.CommandType.response.name(), response);
							
							
							/*returnArray1.add(serverResponse);*/
							returnList.add(serverResponse);
							
							JSONObject returnSize = new JSONObject();
							for(Resource resouce: matchResourceSet){
								JSONObject MatchResouce = new JSONObject();
								
								MatchResouce.put(ConstantEnum.CommandArgument.name.name(), resouce.name);
								JSONArray tagsArray = new JSONArray();
								for (String tag: resouce.tag){
									tagsArray.add(tag);
								}
								MatchResouce.put(ConstantEnum.CommandArgument.tags.name(), tagsArray);
								MatchResouce.put(ConstantEnum.CommandArgument.description.name(), resouce.description);
								MatchResouce.put(ConstantEnum.CommandArgument.uri.name(), resouce.URI);
								MatchResouce.put(ConstantEnum.CommandArgument.channel.name(), resouce.channel);
								
								/**if owner not "", replace it with * */
								if(resouce.owner.equals("")){
									MatchResouce.put(ConstantEnum.CommandArgument.owner.name(), "");
								}else{
									MatchResouce.put(ConstantEnum.CommandArgument.owner.name(), "*");
								}
								
								Integer ezport = serverSocket.getLocalPort();
	
								String ezserver = hostName+":"+ezport.toString();
								MatchResouce.put(ConstantEnum.CommandArgument.ezserver.name(), ezserver);
								
								/*returnArray1.add(MatchResouce);*/
								returnList.add(MatchResouce);
							}
							//serverResponse.put(ConstantEnum.CommandType.resultSize, matchResourceSet.size());
							
							returnSize.put(ConstantEnum.CommandType.resultSize, matchResourceSet.size());
							
							/*returnArray1.add(returnSize);*/
							returnList.add(returnSize);
							/*queryReturn = new QueryReturn(returnArray1);*/
							queryReturn = new QueryReturn(returnList);
							
							hasMacthResource = true;
						}else{							
							errorMessage = "missing resourceTemplate";
							response = "error";
							serverResponse.put(ConstantEnum.CommandType.response.name(),response);
							serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
							queryReturn = new QueryReturn(serverResponse);
						}
						
					}
					
						
				} 
					
				
			
		}while(queryReturn==null);
		return queryReturn;
	}
	
	/**
	 * This method creates the JSON object to be returned to client, in response to Exchange command.
	 * @param serverList
	 * @param serverList_exchange
	 * @param hostnameList_exchange
	 * @param portList_exchange
	 * @return the JSON object to be returned to client
	 */
	public synchronized static JSONObject handlingExchange(ArrayList<String> serverList, ArrayList<String>serverList_exchange, 
			ArrayList<String> hostnameList_exchange, ArrayList<String> portList_exchange){
			JSONObject serverResponse = new JSONObject();
			String response;
			String errorMessage;
			//in fact, the hostnamePattern cannot find the error in ip format like 999.1234.999.1, because the pattern must fit hostname format.
			String hostnamePattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
			String portPattern = "^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$";
			if(serverList_exchange.size()==0){
				response="error";
				errorMessage = "missing or invalid server list";	
				serverResponse.put(ConstantEnum.CommandType.response.name(),response);
				serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
				return serverResponse;
			}
			for(int i=0; i<serverList_exchange.size(); i++){
				if(!(Pattern.matches(hostnamePattern,hostnameList_exchange.get(i))
						&&Pattern.matches(portPattern, portList_exchange.get(i)))){//doesn't fits rexp
					response="error";
					errorMessage = "missing resourceTemplate";	
					serverResponse.put(ConstantEnum.CommandType.response.name(),response);
					serverResponse.put(ConstantEnum.CommandArgument.errorMessage.name(), errorMessage);
					return serverResponse;
				}
				else if (!serverList.contains(serverList_exchange.get(i))){
					serverList.add(serverList_exchange.get(i));
				}
			}
			response="success";
			serverResponse.put(ConstantEnum.CommandType.response.name(),response);
			return serverResponse;
			
		}
	
	/**
	 * This method creates the JSON object to be returned to client, in response to Query command with Relay field set as TRUE.
	 * @param inputMessage
	 * @param resources
	 * @param serverSocket
	 * @param serverList
	 * @param hasDebugOption
	 * @return the JSON object to be returned to client
	 */
	public synchronized static QueryData handlingQueryWithRelay(String inputMessage,HashMap<String, Resource> resources, 
			ServerSocket serverSocket, ArrayList<String> serverList, boolean hasDebugOption){
			JSONObject inputQuerry = new JSONObject();
			ArrayList<JSONObject> arrayList = new ArrayList<>();
			QueryData otherReturn = new QueryData();
			int totalOtehrResSize = 0;
			boolean hasMatchServer = false;
			/**parse input query from the client*/
			try {
				JSONParser parser = new JSONParser();
				inputQuerry = (JSONObject) parser.parse(inputMessage);
				
			} catch (org.json.simple.parser.ParseException e) {
				e.printStackTrace();
			}
			
			/**replace owner, channel with"" and set relay with true, then forward query*/
			
			inputQuerry.put("channel", "");
			/*inputQuerry.put("owner", "");*/
			inputQuerry.put("relay", "false");
					
			/**a list to store success information from other servers*/
			ArrayList<JSONObject> successOutcome = new ArrayList<>();
			ArrayList<JSONObject> errorOutcome = new ArrayList<>();			
			
			if(!serverList.isEmpty()){
			
					for(String server: serverList){
						
						String[] hostAndPortTemp = server.split(":");
						String tempIp = hostAndPortTemp[0];
						Integer tempPort = Integer.parseInt(hostAndPortTemp[1]);
						try {
							/**not query server itself while relay is true*/
							if(!InetAddress.getLocalHost().getHostAddress().equals(tempIp)){
								try {
									Socket otherServer = new Socket(tempIp, tempPort);
									DataInputStream inputStream = new DataInputStream(otherServer.getInputStream());
									DataOutputStream outputStream = new DataOutputStream(otherServer.getOutputStream());
									outputStream.writeUTF(inputQuerry.toJSONString());
									outputStream.flush();
									if(hasDebugOption){
										System.out.println("SENT: "+inputQuerry.toJSONString());
									}
									System.out.println("query sent to other server");
									StopWatch s = new StopWatch();
									s.start();
									while(true){
										if(inputStream.available()>0){
											String otherServerResponse = inputStream.readUTF();
											JSONParser parser2 = new JSONParser();
											
											JSONObject otherResponse = new JSONObject();
											otherResponse = (JSONObject)parser2.parse(otherServerResponse);
											/*System.out.println(otherResponse.toJSONString());*/
											JSONArray  jsonArray = new JSONArray();
											
											arrayList.add((JSONObject)parser2.parse(otherServerResponse));
											
											if(otherResponse.containsKey("resultSize")||otherResponse.containsKey("errorMessage")){
												break;
											}
											
										}
										/**other server connected but no response*/
										if(s.getTime()>500){
											s.stop();
											return otherReturn;
										}
									}
									
										if (arrayList.get(0).get("response").equals("success")) {
											hasMatchServer =true;
											int size = arrayList.size();
											totalOtehrResSize = totalOtehrResSize+size;
											
											for(int i =1; i<size-1;i++){
												successOutcome.add(arrayList.get(i));
												
											}
											otherReturn = new QueryData(true, successOutcome);
											
											
										}else{
											int size = arrayList.size();
											for(int i = 0;i<size;i++){
												errorOutcome.add(arrayList.get(i));
											}
											otherReturn = new QueryData(false, errorOutcome);
											
										}	
									
										
										
											
													
										} catch (Exception e) {
											
											e.printStackTrace();
										}
							}
								
							
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						}
				} 
				return otherReturn;
			}
	


}
