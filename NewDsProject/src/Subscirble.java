import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Subscirble {
	private HashMap<String, Resource> lastState;
	private HashMap<String, Resource> resources;
	private boolean updated = false;
	DataInputStream in;
	DataOutputStream out;
	boolean hasDebugOption;
	
	public Subscirble(HashMap<String, Resource> resources,DataInputStream in,DataOutputStream out,boolean hasDebugOption){
		this.resources = resources;
		this.lastState = new HashMap<>(resources);
		this.in = in;
		this.out = out;
		this.hasDebugOption = hasDebugOption;
	}
	
	
	public synchronized static void handlingSubscribleTest(JSONObject input,DataInputStream in,DataOutputStream out,
			ServerSocket socket,HashMap<String, Resource> resources,String hostName, boolean hasDebugOption){
		JSONObject template_resource_sub = (JSONObject)input.get("resourceTemplate");
		JSONArray debugMsg_sub = new JSONArray();
		boolean relay;
		ArrayList<Resource> matchList = new ArrayList<>();
		boolean isUnsubscribe =false;
		
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
		try {
			isUnsubscribe = unsubscribe.get();
		} catch (Exception e) {
			
		}
		//loop until receive unsubscribe message
		while(isUnsubscribe==false){
			if (relay ==false) {
				QueryReturn queryReturn= ServerHandler.handlingSubscribe(id, name, tags, description, uri, channel, owner, relay, resources, socket, hostName);
				Subscirble subscirble = new Subscirble(resources,in,out,hasDebugOption);
				
				//resourse template missing filed or invalid
				if (queryReturn.hasMatch==false&&queryReturn.reponseMessage.get("response").toString().equals("error")) {
					subscirble.sendMessage(queryReturn.reponseMessage);
				}else{
					//valid template, match or waiting update
					
					while(subscirble.updated==true){
						
					}
				}
				
				
				
				
				
				
				
				
				
			}else{
				
				
			}
			
			
			
			
			
		}
		
		
		
		
	}
	
	public void checkUpdates(){
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
			checkUpdated();				
			}
		}, 1000, 1000);
	}
	
	private boolean checkUpdated() {
	    this.updated = !this.resources.equals(this.lastState);
	    this.lastState.clear();
	    this.lastState.putAll(resources);
	    return this.updated;
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
