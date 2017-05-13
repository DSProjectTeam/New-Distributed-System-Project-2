import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.time.StopWatch;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Subscrible {
	private HashMap<String, Resource> lastState;
	private HashMap<String, Resource> resources;
	private ArrayList<String> lastStateServerList;
	private ArrayList<String> serverList;
	private static ArrayList<String> newServers;
	private boolean updated = false;
	private boolean serverListUpdated = false;
	DataInputStream in;
	DataOutputStream out;
	boolean hasDebugOption;
	ArrayList<JSONObject> matchList;
	int hitCounter;
	static int relayHitCounter;
	
	public Subscrible(HashMap<String, Resource> resources,ArrayList<String> serverList, DataInputStream in,DataOutputStream out,boolean hasDebugOption){
		this.resources = resources;
		this.lastState = new HashMap<>(resources);
		this.serverList = serverList;
		this.lastStateServerList = new ArrayList<String>(serverList);
		this.newServers.clear();
		this.in = in;
		this.out = out;
		this.hasDebugOption = hasDebugOption;
		this.matchList = new ArrayList<>();
		int hitCounter = 0;
		this.relayHitCounter = 0;
	}
	
	
	public synchronized static void handlingSubscribleTest(JSONObject input,DataInputStream in,DataOutputStream out,
			ServerSocket socket,HashMap<String, Resource> resources,String hostName, boolean hasDebugOption, ArrayList<String> serverList){
		JSONObject template_resource_sub = (JSONObject)input.get("resourceTemplate");
		JSONArray debugMsg_sub = new JSONArray();
		boolean relay;
		boolean isUnsubscribe =false;
		boolean forwarded = false;
		boolean unsubscriptionForwarded = false;
		
		String [] tags = ServerThread.handleTags(template_resource_sub.get(ConstantEnum.CommandArgument.tags.name()).toString());
		String name = template_resource_sub.get(ConstantEnum.CommandArgument.name.name()).toString();
		String description = template_resource_sub.get(ConstantEnum.CommandArgument.description.name()).toString();
		String uri = template_resource_sub.get(ConstantEnum.CommandArgument.uri.name()).toString();
		String channel = template_resource_sub.get(ConstantEnum.CommandArgument.channel.name()).toString();
		String owner = template_resource_sub.get(ConstantEnum.CommandArgument.owner.name()).toString();
		String relay_sub= input.get(ConstantEnum.CommandArgument.relay.name()).toString();
		String id = input.get(ConstantEnum.CommandArgument.id.name()).toString();
		if(relay_sub.equals("")){
			relay = true;
		}else{
			relay = Boolean.parseBoolean(relay_sub);
		}
		
		//create new callabe thread to monitor unsubscribe status
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<Boolean> unsubscribe = executorService.submit(new IsSubscribe(in, id));
		
		//loop until receive unsubscribe message
		if (relay == false) {
			
			while(isUnsubscribe == false){
				
				QueryReturn queryReturn= ServerHandler.handlingSubscribe(id, name, tags, description, uri, channel, owner, relay, resources, socket, hostName);
				Subscrible Subscrible = new Subscrible(resources,serverList,in,out,hasDebugOption);
				
				//invalid template or valid template but no current match, pending.
				if (queryReturn.hasMatch==false) {
					
					//invalid template
					if (queryReturn.reponseMessage.get("response").toString().equals("error")) {
						Subscrible.sendMessage(queryReturn.reponseMessage);
						break;
					}else{
						//valid template, but no match currently, pending
						if (queryReturn.reponseMessage.get("response").toString().equals("pending")) {
							Subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName);
							System.out.println("pending!!");
						}
					}
				}else{
					//valid template, has match, monitor resources update.
	
						for(JSONObject jsonObject: queryReturn.returnList){
							try {
								out.writeUTF(jsonObject.toJSONString());
								
								//put it in the match list to avoid duplicate resource has been sent
								Subscrible.matchList.add(jsonObject);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}
						Subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName);
					
				}
				
				
				//if unsubscribe, break the loop, return result size
				try {
					isUnsubscribe = unsubscribe.get();
					if (isUnsubscribe) {
						//remove the {"id":xxx} or {"resposne":"success"}
						Subscrible.matchList.remove(0);
						
						JSONObject jsonObject = new JSONObject();
						System.out.println(Subscrible.matchList.size());
						jsonObject.put("resultSize", Subscrible.matchList.size());
						out.writeUTF(jsonObject.toJSONString());		
						out.flush();
						Thread.currentThread().yield();
						break;
					}
				} catch (Exception e) {
					
				}
				
			}
		}else{
			//relay is true
			
			
			
			
			while(isUnsubscribe==false){
				QueryReturn queryReturn= ServerHandler.handlingSubscribe(id, name, tags, description, uri, channel, owner, relay, resources, socket, hostName);
				Subscrible Subscrible = new Subscrible(resources,serverList,in,out,hasDebugOption);
				
				//invalid template or valid template but no current match, pending.
				if (queryReturn.hasMatch==false) {
					
					//invalid template
					if (queryReturn.reponseMessage.get("response").toString().equals("error")) {
						Subscrible.sendMessage(queryReturn.reponseMessage);
						break;
					}else{
						//valid template, but no match currently, pending
						if (queryReturn.reponseMessage.get("response").toString().equals("pending")) {
							Subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName);
							System.out.println("pending!!");
						}
					}
				}else{
					//valid template, has match, monitor resources update.

						for(JSONObject jsonObject: queryReturn.returnList){
							try {
								out.writeUTF(jsonObject.toJSONString());
								
								//put it in the match list to avoid duplicate resource has been sent
								Subscrible.matchList.add(jsonObject);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}
						Subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName);
					
				}
				
				
				
				//below begin to forward the subscription to other servers.
				if(forwarded==false){
					if(!serverList.isEmpty()){
						//change relay field to false.
						input.put("relay", "false");						
						ExecutorService executorServiceForward = Executors.newFixedThreadPool(serverList.size());
						for(String server: serverList){
							String[] hostAndPortTemp = server.split(":");
							String tempIP = hostAndPortTemp[0];
							Integer tempPort = Integer.parseInt(hostAndPortTemp[1]);

							try {
								if(!InetAddress.getLocalHost().getHostAddress().equals(tempIP)){
									WaitSubRelay2 relay2 = new WaitSubRelay2(input, tempIP, tempPort, out, id, relayHitCounter);
									relay2.run();
									
								/*	Future<Integer> hitCount = executorServiceForward.submit(new WaitSubRelayResponse(input, 
											tempIP, tempPort,out, id));
										//Future返回如果没有完成，则一直循环等待，直到Future返回完成
										while(!hitCount.isDone());
										relayHitCounter += hitCount.get();	*/
								}
							} catch (Exception e) {
								e.printStackTrace();
							}		
						}
					} 
					forwarded = true;
				}
				
				//below check serverList update and forward to them, also close threads to no-existed servers.
				else{//forwarded==true, forwarded already
					if (!newServers.isEmpty()){	
						input.put("relay", "false");						
						ExecutorService executorServiceForward = Executors.newFixedThreadPool(newServers.size());
						for(String server:newServers){
								String[] hostAndPortTemp = server.split(":");
								String tempIP = hostAndPortTemp[0];
								Integer tempPort = Integer.parseInt(hostAndPortTemp[1]);

								try {
									if(!InetAddress.getLocalHost().getHostAddress().equals(tempIP)){
											/*Future<Integer> hitCount = executorServiceForward.submit(new WaitSubRelayResponse(input, 
												tempIP, tempPort,out, id));
											//Future返回如果没有完成，则一直循环等待，直到Future返回完成
											while(!hitCount.isDone());
											relayHitCounter += hitCount.get();	*/
										WaitSubRelay2 relay2 = new WaitSubRelay2(input, tempIP, tempPort, out, id, relayHitCounter);
										relay2.run();
									}
								} catch (Exception e) {
									e.printStackTrace();
								}		
						}
						newServers.clear();
					}
				}
				
				//once unsubscribe, break the loop, return result size
				try {
					StopWatch swatch = new StopWatch();
					isUnsubscribe = unsubscribe.get();
					if ((isUnsubscribe==true)&&(unsubscriptionForwarded==false)) {
						//remove the {"id":xxx} or {"resposne":"success"}
						Subscrible.matchList.remove(0);
						//send unsubscribe JSONObjects to all the servers. the responses from the servers containing resultSizes will be caught in the while loop above.
						for(String server: serverList){
							String[] hostAndPortTemp = server.split(":");
							String tempIP = hostAndPortTemp[0];
							Integer tempPort = Integer.parseInt(hostAndPortTemp[1]);
							if(!InetAddress.getLocalHost().getHostAddress().equals(tempIP)){
								JSONObject UnsubJSONObject = new JSONObject();
								UnsubJSONObject.put("command", "UNSUBSCRIBE");
								UnsubJSONObject.put("id", id);
								Socket serverSocket = new Socket(tempIP,tempPort);
								DataOutputStream serversOut = new DataOutputStream(serverSocket.getOutputStream());
								serversOut.writeUTF(UnsubJSONObject.toJSONString());
								serversOut.flush();
							}
							unsubscriptionForwarded = true;
						}
						swatch.start();
					}
//						handlingUnsubscribe((ArrayList<String> subscriberList, String id, JSONObject jsonObject,HashMap<String, Resource> resources, 
//								ServerSocket serverSocket, ArrayList<String> serverList, boolean hasDebugOption));
						
					//if unsubscription have been sent, then wait 1300ms to receive resultSizes from server to be responded.
					if ((isUnsubscribe==true)&&(unsubscriptionForwarded==true)&&swatch.getTime()>1300) {
						JSONObject jsonObject = new JSONObject();
						System.out.println("hits from local servers"+Subscrible.matchList.size()+"total hits from other servers"+relayHitCounter);
						jsonObject.put("resultSize", Subscrible.matchList.size()+relayHitCounter);
						out.writeUTF(jsonObject.toJSONString());		
						out.flush();//which out?
						Thread.currentThread().yield();
						break;
					}
//					}
				} catch (Exception e) {
					
				}
			}
			
		}
		
		
		
		
	}
	
	
	/**
	 * monitor the status of the resources hashmap, if has changed and match resource template. write output.
	 * */
	public void checkUpdates(String id, String name,String[] tags,String description,String uri,String channel,String owner,
			boolean relay,ServerSocket socket,String hostName){
			new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
			checkUpdated(id, name, tags, description, uri, channel, owner, relay, socket, hostName);
			checkUpdatedServer();
			}
		}, 1000, 1000);
	}
	
	private boolean checkUpdated(String id, String name,String[] tags,String description,String uri,String channel,String owner,
			boolean relay,ServerSocket socket,String hostName) {
	   
		if(!this.resources.equals(this.lastState)){
			System.out.println("updated");
			QueryReturn temp = ServerHandler.handlingSubscribe(id, name, tags, description, uri, channel, owner, relay, this.resources, socket, hostName);
			if (temp.hasMatch==true) {
				for(JSONObject jsonObject : temp.returnList){
					
					if (matchList.isEmpty()) {
						try {
							this.out.writeUTF(jsonObject.toJSONString());
							this.hitCounter++;
						} catch (IOException e) {
							e.printStackTrace();
						}
						matchList.add(jsonObject);
					}else{
						if(!matchList.contains(jsonObject)){
							try {
								this.out.writeUTF(jsonObject.toJSONString());
								this.hitCounter++;
							} catch (IOException e) {
								e.printStackTrace();
							}
							matchList.add(jsonObject);
						}
					}					
				}
			}
		}
		
	    this.lastState.clear();
	    this.lastState = new HashMap<>(this.resources);
	    return this.updated;
	}
	
	
	/**
	 * check updates in the serverList, add newly added servers to newServers ArrayList.
	 * @return if serverList has updated.
	 */
	private boolean checkUpdatedServer() {
		this.newServers.clear();
        if(this.serverList.size()!=this.lastStateServerList.size()){
        	System.out.println("serverList updated");
        	this.serverListUpdated = false;
        }
        for(String server: this.serverList)
        {
            if(!this.lastStateServerList.contains(server)){
            	System.out.println("serverList updated");
            	this.newServers.add(server);
            	this.serverListUpdated = false;
            }
                
        }
        this.serverListUpdated = true;
		
	    this.lastStateServerList.clear();
	    this.lastStateServerList = new ArrayList<String>(this.serverList);
	    return this.serverListUpdated;
	}
	
	
	/**
	 * This method sends response message from the server to client
	 * @param message
	 */
	public synchronized void sendMessage(JSONObject message){
		try {
			
			out.writeUTF(message.toJSONString());
			out.flush();
			if(hasDebugOption){
			       System.out.println("SENT: "+message.toJSONString());
				}
			System.out.println(Thread.currentThread().getName()+":sending response message!");
			
		} catch (IOException e) {
			System.err.println(Thread.currentThread().getName() + ":Error while sending");
		}
	}
	
	
}
