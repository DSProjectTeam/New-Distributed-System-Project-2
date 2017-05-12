import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**this class is used to monitor if received unsubscribe command */

public class WaitSubRelayResponse implements Callable<Integer>{
	DataInputStream in;
	String id;
	String host;
	int port;
	String clientHost;
	int clientPort;
	JSONObject subscribeRequest;
	boolean isUnsubscribe = false;
	
	public WaitSubRelayResponse(JSONObject subscribeRequest, String host, int port, 
			String clientHost, int clientPort,DataInputStream in,DataInputStream out,String id) {
		this.clientHost = clientHost;
		this.clientPort = clientPort;
		this.subscribeRequest = subscribeRequest;
		this.host = host;
		this.port = port;
		this.in = in;
		this.id = id;
	}
	
	
	
	@Override
	public Integer call() {
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
					if (!message.get("resultSize").toString().equals(null)) {
						hitCount = Integer.parseInt(message.get("resultSize").toString());
						break;
					}
					if (!message.get("name").toString().equals(null)){//这样就能确定是resource了吗？？
						//set the client socket to connect to.
						Socket clientSocket = new Socket(clientHost,clientPort);
						DataOutputStream clientOut = new DataOutputStream(clientSocket.getOutputStream());
						clientOut.writeUTF(message.toJSONString());
						clientOut.flush();
					}
					if(message.get("name").toString().equals("error")){
						System.out.println(message.toJSONString());
						//other situation:error
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hitCount;
		
	}
	
}


