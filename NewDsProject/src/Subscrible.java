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
//	private static ArrayList<String> newServers;
	private boolean updated = false;
	private boolean serverListUpdated = false;
	DataInputStream in;
	DataOutputStream out;
	boolean hasDebugOption;
	boolean isSecurePort;
	ArrayList<JSONObject> matchList;
	ArrayList<JSONObject> matchBeforeChangeList;
	int hitCounter;
	volatile static int relayHitCounter = 0;
	
	public Subscrible(HashMap<String, Resource> resources,ArrayList<String> serverList, DataInputStream in,DataOutputStream out,boolean hasDebugOption){
		this.resources = resources;
		this.lastState = new HashMap<>(resources);
		this.serverList = serverList;
		this.lastStateServerList = new ArrayList<String>(serverList);
//		this.newServers.clear();
//		this.newServers =new ArrayList<String>();
		this.in = in;
		this.out = out;
		this.hasDebugOption = hasDebugOption;
		this.matchList = new ArrayList<>();
		int hitCounter = 0;
		//this.relayHitCounter = 0;
		this.isSecurePort = isSecurePort;
		this.matchBeforeChangeList = new ArrayList<>();
	}
	
	
	public Subscrible(HashMap<String, Resource> resources, DataInputStream in,DataOutputStream out,boolean hasDebugOption){
		this.resources = resources;
		this.lastState = new HashMap<>(resources);
		this.in = in;
		this.out = out;
		this.hasDebugOption = hasDebugOption;
		this.matchList = new ArrayList<>();
		int hitCounter = 0;
		this.matchBeforeChangeList = new ArrayList<>();
	}
	
	
	public synchronized static void handlingSubscribleTest(JSONObject input,DataInputStream in,DataOutputStream out,
			ServerSocket socket,HashMap<String, Resource> resources,String hostName, boolean hasDebugOption, ArrayList<String> serverList,
			boolean isSecurePort){
		JSONObject template_resource_sub = (JSONObject)input.get("resourceTemplate");
		JSONArray debugMsg_sub = new JSONArray();
		boolean relay;
		boolean isUnsubscribe =false;
		boolean forwarded = false;
		boolean unsubscriptionForwarded = false;
		//int counter = 0;
		
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
		Future<Boolean> unsubscribe = executorService.submit(new IsSubscribe(in, id,hasDebugOption,isSecurePort));
		
		//loop until receive unsubscribe message
		if (relay == false) {
//			System.out.println("1");
			if(isUnsubscribe == false){
				
				QueryReturn queryReturn= ServerHandler.handlingSubscribe(id, name, tags, description, uri, channel, owner, relay, resources, socket, hostName);
				//Subscrible Subscrible = new Subscrible(resources,serverList,in,out,hasDebugOption);
				//invalid template or valid template but no current match, pending.
				Subscrible subscrible = new Subscrible(resources, in, out, hasDebugOption);
				if (queryReturn.hasMatch==false) {
//					System.out.println("2");
					//invalid template
					if (queryReturn.reponseMessage.get("response").toString().equals("error")) {
						subscrible.sendMessage(queryReturn.reponseMessage);
//						break;///////////////////
					}else{
						//valid template, but no match currently, pending
						if (queryReturn.reponseMessage.get("response").toString().equals("pending")) {
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("response", "success");
							jsonObject.put("id", id);
							subscrible.sendMessage(jsonObject);
							subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName,input, subscrible, unsubscribe);
						}
					}
				}else{
					//valid template, has match, monitor resources update.
					for(JSONObject jsonObject: queryReturn.returnList){
						
						//local has match when subscribe come in, these matches will not be sent.
						subscrible.matchBeforeChangeList.add(jsonObject);
						
						
					}
					
					


					/*System.out.println("3");
						for(JSONObject jsonObject: queryReturn.returnList){
							try {
								out.writeUTF(jsonObject.toJSONString());
								
								//put it in the match list to avoid duplicate resource has been sent
								subscrible.matchList.add(jsonObject);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}*/
						subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName,input, subscrible, unsubscribe);

					
				}
				
				System.out.println("asfasf");
				//if unsubscribe, break the loop, return result size
				while(true){
					if(unsubscribe.isDone()){
						try {System.out.println(unsubscribe.isDone());
							isUnsubscribe = unsubscribe.get();System.out.println(unsubscribe.isDone());
							if (isUnsubscribe) {
								JSONObject jsonObject = new JSONObject();
								/*if(subscrible.matchList.size()>1){
									
									//remove the {"id":xxx} or {"resposne":"success"}
									subscrible.matchList.remove(0);
									System.out.println(subscrible.matchList.toString());
									jsonObject.put("resultSize", subscrible.matchList.size());
								}
								else{
									jsonObject.put("resultSize", subscrible.matchList.size());
								}*/
								jsonObject.put("resultSize", subscrible.hitCounter);
								subscrible.sendMessage(jsonObject);
		
		//						out.writeUTF(jsonObject.toJSONString());		
		//						out.flush();
								
								//Thread.yield();
								break;//////////////////////////
							}
						} catch (Exception e) {
							
						}
					}//if isDone ends.
				}//while true ends
			}
		}else{
			//relay is true, not in a loop
			
				System.out.println("1");
				QueryReturn queryReturn= ServerHandler.handlingSubscribe(id, name, tags, description, uri, channel, owner, relay, resources, socket, hostName);System.out.println("1.1");System.out.println("1.1");
				Subscrible Subscrible = new Subscrible(resources,serverList,in,out,hasDebugOption);System.out.println("1.2");
				
				//invalid template or valid template but no current match, pending.
				if (queryReturn.hasMatch==false) {System.out.println("2");
					
					//invalid template
					if (queryReturn.reponseMessage.get("response").toString().equals("error")) {
						Subscrible.sendMessage(queryReturn.reponseMessage);
//						break;
					}else{
						//valid template, but no match currently, pending
						if (queryReturn.reponseMessage.get("response").toString().equals("pending")) {
							Subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName,input, Subscrible, unsubscribe);
							System.out.println("pending!!");
						}
					}
				}else{System.out.println("3");
					//valid template, has match, monitor resources update.

						for(JSONObject jsonObject: queryReturn.returnList){
							/*try {
								out.writeUTF(jsonObject.toJSONString());
								
								//put it in the match list to avoid duplicate resource has been sent
								Subscrible.matchList.add(jsonObject);
							} catch (IOException e) {
								e.printStackTrace();
							}*/
							Subscrible.matchBeforeChangeList.add(jsonObject);
							
						}
						Subscrible.checkUpdates(id, name, tags, description, uri, channel, owner, relay,socket, hostName,input, Subscrible, unsubscribe);
					
				}
				
				if(!serverList.isEmpty()){System.out.println("4");
					//change relay field to false.
					input.put("relay", "false");						
					ExecutorService executorServiceForward = Executors.newFixedThreadPool(serverList.size());
					for(String server: serverList){
						String[] hostAndPortTemp = server.split(":");
						String tempIP = hostAndPortTemp[0];
						Integer tempPort = Integer.parseInt(hostAndPortTemp[1]);
						
						try {
							if(!InetAddress.getLocalHost().getHostAddress().equals(tempIP)){System.out.println("去监听了");
								
								//WaitSubRelay2 现在自己可以监听client端的unsubscribe命令。
								WaitSubRelay2 relay2 = new WaitSubRelay2(input, tempIP, tempPort, out, id, Subscrible,in,isSecurePort,hasDebugOption,unsubscribe);
								//relay2.run();
								System.out.println("12");
								new Thread(relay2).start();
								//counter++;
								
								System.out.println("12");
								
							}
						} catch (Exception e) {
							e.printStackTrace();
						}		
					}
				} 

				
				while(true){
					if(unsubscribe.isDone()){
						try{
							isUnsubscribe = unsubscribe.get();
							//System.out.println(unsubscribe.isDone());
							//System.out.println("7");
							if (isUnsubscribe) {System.out.println("8");
							StopWatch omega = new StopWatch();
							omega.start();
							while(omega.getTime()<1000){
								
							}
							omega.stop();
//								StopWatch watch = new StopWatch();
//								watch.start();
								
								/*这里让下面的代码暂停0.5秒再执行，以免WaitRelay2中对relayHitCounter的操作还没更新
								 * 而下面代码已经将relayHitCounter发送走了。
								 * */
//								if (watch.getTime()>500) {
//									watch.stop();
									//remove the {"id":xxx} or {"resposne":"success"}
							
									if(!Subscrible.matchList.isEmpty()){
										Subscrible.matchList.remove(0);
									}

									
									JSONObject jsonObject = new JSONObject();
									System.out.println("hits from local servers"+Subscrible.matchList.size()+"total hits from other servers"+relayHitCounter);
									
									jsonObject.put("resultSize", Subscrible.matchList.size()+relayHitCounter);
									out.writeUTF(jsonObject.toJSONString());		
									out.flush();
									Thread.yield();
									break;
//								}
								
							}
						}catch(Exception e){
						}
					}
				}
				//System.out.println("counter "+counter);
				
			
		
		}
		
		
		
	}
	
	
	/**
	 * monitor the status of the resources hashmap, if has changed and match resource template. write output.
	 * */
	public void checkUpdates(String id, String name,String[] tags,String description,String uri,String channel,String owner,
			boolean relay,ServerSocket socket,String hostName,JSONObject input,Subscrible sub,Future<Boolean> unsubscribe){
			new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
			checkUpdated(id, name, tags, description, uri, channel, owner, relay, socket, hostName);
			if (relay==true) {
				//checkUpdatedServer(input,id,sub,unsubscribe);
			}
			
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
					
					if (matchList.isEmpty()&&!this.matchBeforeChangeList.contains(jsonObject)&&!jsonObject.containsKey("response")) {
						try {
							this.out.writeUTF(jsonObject.toJSONString());//delete this line to avoid duplicate?
							this.hitCounter++;
							if(hasDebugOption){
							       System.out.println("SENT: "+jsonObject.toJSONString());
								}
						} catch (IOException e) {
							e.printStackTrace();
						}
						matchList.add(jsonObject);
					}else{
						if(!matchList.contains(jsonObject)&&!this.matchBeforeChangeList.contains(jsonObject)&&!jsonObject.containsKey("response")){
							try {
								this.out.writeUTF(jsonObject.toJSONString());
								this.hitCounter++;
								if(hasDebugOption){
								       System.out.println("SENT: "+jsonObject.toJSONString());
									}
								matchList.add(jsonObject);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
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
	private boolean checkUpdatedServer(JSONObject input, String id,Subscrible sub,Future<Boolean> unsubscribe) {
//		newServers.clear();
        if(this.serverList.size()!=this.lastStateServerList.size()){
        	System.out.println("serverList updated");
//        	this.serverListUpdated = false;
        }
        for(String server: this.serverList)
        {
            if(!this.lastStateServerList.contains(server)){
            	System.out.println("serverList updated");
				input.put("relay", "false");						
				ExecutorService executorServiceForward = Executors.newFixedThreadPool(1);
				
				String[] hostAndPortTemp = server.split(":");
				String tempIP = hostAndPortTemp[0];
				Integer tempPort = Integer.parseInt(hostAndPortTemp[1]);

				try {
					if(!InetAddress.getLocalHost().getHostAddress().equals(tempIP)){
							
						WaitSubRelay2 relay2 = new WaitSubRelay2(input, tempIP, tempPort, out, id, sub,in,isSecurePort,hasDebugOption,unsubscribe);
						new Thread(relay2).start();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}		
				
//            	this.newServers.add(server);
//            	this.serverListUpdated = false;
            }
                
        }
//        this.serverListUpdated = true;
		
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
