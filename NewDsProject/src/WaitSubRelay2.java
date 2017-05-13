import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class WaitSubRelay2 implements Runnable{

	String id;
	String host;
	int port;
	DataOutputStream clientOut;
	JSONObject subscribeRequest;
	boolean isUnsubscribe = false;
	int relayHitCounter;
	
	public WaitSubRelay2(JSONObject subscribeRequest, String host, int port, 
			DataOutputStream clientOut,String id, int relayHitCounter ) {
		this.subscribeRequest = subscribeRequest;
		this.host = host;
		this.id = id;
		this.clientOut = clientOut;
		this.relayHitCounter = relayHitCounter;
		
	}
	
	@Override
	public void run() {
		int hitCount = 0;
		try {
			//set server socket to connect to.
			Socket socket = new Socket(host,port);
			
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());//应该用哪个out呀？？
			out.writeUTF(subscribeRequest.toJSONString());
			out.flush();
			
			DataInputStream in = new DataInputStream(socket.getInputStream());//?应该用哪个in呀？
			while(true){
				if (in.available()>0) {
					JSONParser parser = new JSONParser();
					JSONObject message = (JSONObject) parser.parse(in.readUTF());
					
					while(!message.containsKey("resultSize")){
						clientOut.writeUTF(message.toJSONString());
						clientOut.flush();
					}
					hitCount = Integer.parseInt(message.get("resultSize").toString());
					relayHitCounter = relayHitCounter+hitCount;
					break;
				}
			}
			
			Thread.yield();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
