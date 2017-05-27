import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.time.StopWatch;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**this class is used to monitor if received unsubscribe command */
public class WaitSubRelay2 implements Runnable{

	String id;
	String host;
	int port;
	DataOutputStream clientOutput;
	DataInputStream clientInput;
	DataOutputStream out;
	DataInputStream in;
	JSONObject subscribeRequest;
	//volatile int relayHitCounter;
	boolean isUnsubscribe = false;
	boolean isSecurePort;
	boolean hasDebugOption;
	Future<Boolean> unsubscribe;
	Subscrible sub;
	
	public WaitSubRelay2(JSONObject subscribeRequest, String host, int port, 
			DataOutputStream clientOut,String id, Subscrible sub, DataInputStream clientInput, boolean isSecurePort,boolean hasDebugOption,
			Future<Boolean> unsubscribe) {
		this.subscribeRequest = subscribeRequest;
		this.host = host;
		this.port = port;
		this.id = id;
		this.clientOutput = clientOut;
		this.clientInput = clientInput;
		//this.relayHitCounter = relayHitCounter;
		this.isSecurePort = isSecurePort;
		this.hasDebugOption = hasDebugOption;
		this.unsubscribe = unsubscribe;
		this.sub = sub;
	}
	
	@Override
	public void run() {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
		//here we also use IsSubscribe to listen to unsubscribe
		/*ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<Boolean> unsubscribe = executorService.submit(new IsSubscribe(clientInput, id,hasDebugOption));*/
		
		int hitCount = 0;
		

			try {
				//set server socket to connect to secure server
				if(isSecurePort){
					System.setProperty("javax.net.ssl.trustStore", "sslconnection/clientKeystore.jks");
					System.setProperty("javax.net.ssl.keyStore","sslconnection/clientKeystore.jks");
					System.setProperty("javax.net.ssl.keyStorePassword", "12345678");
					SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
					SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);
					
					//set socket time out to implement in.avaliable()
					sslSocket.setSoTimeout(1300);
					
					out = new DataOutputStream(sslSocket.getOutputStream());
					in = new DataInputStream(sslSocket.getInputStream());
					
				}else{
					//unsecure connection
					Socket socket = new Socket(host,port);
					out = new DataOutputStream(socket.getOutputStream());
					in = new DataInputStream(socket.getInputStream());
				}
				
				
				
				
				out.writeUTF(subscribeRequest.toJSONString());
				out.flush();
				
				StopWatch swatch = new StopWatch();
				
				/*after sent unsubscribe comamnd to other servers, extract resultSize.
				Otherwise, just forward the resources received from other servers to the client as normal.
				*/

				while(true){
					if(unsubscribe.isDone()){
						//listen to unsubscribe command
						//isUnsubscribe = unsubscribe.get();
						//if (isUnsubscribe==true) {
							JSONObject UnsubJSONObject = new JSONObject();
							UnsubJSONObject.put("command", "UNSUBSCRIBE");
							UnsubJSONObject.put("id", id);
							out.writeUTF(UnsubJSONObject.toJSONString());
							//System.out.println("666666666");
							
						//}
					}
					//input > 0
					/*if (in.available()>0) {
						JSONParser parser = new JSONParser();
						JSONObject message = (JSONObject) parser.parse(in.readUTF());
						
						if(!message.containsKey("resultSize")){
							clientOutput.writeUTF(message.toJSONString());
							clientOutput.flush();
						}else{
							hitCount = Integer.parseInt(message.get("resultSize").toString());
							relayHitCounter = relayHitCounter+hitCount;
							break;
						}
						
						
					}*/
					//System.out.println(isSecurePort+ "  !");
					JSONParser parser = new JSONParser();
					//if secure connection,  in.avaliable is not working, try to catch socketTimeout exception to 
					//replace it with the similar function.
					if(isSecurePort){
						try{
							JSONObject message = (JSONObject) parser.parse(in.readUTF());
							if(!message.containsKey("resultSize")){
								clientOutput.writeUTF(message.toJSONString());
								clientOutput.flush();
							}else{
								hitCount = Integer.parseInt(message.get("resultSize").toString());
								/*int temp = sub.relayHitCounter;
								temp = temp+ hitCount;
								sub.relayHitCounter = temp;*/
								sub.relayHitCounter = sub.relayHitCounter+hitCount;
								//System.out.println(message.toJSONString()+"  "+sub.relayHitCounter+"  "+temp);
//								System.out.println(message.toJSONString()+"  "+sub.relayHitCounter+"  ");
								//Thread.currentThread().notify();
								
								Thread.yield();
								break;
							}
						}catch(SocketTimeoutException e){
							//should NOT be any "break" here!, the while(true) loop will continue.
						}

					}else{
						if (in.available()>0) {
							
							JSONObject message = (JSONObject) parser.parse(in.readUTF());
//							System.out.println(message.toJSONString()+"wawawawawa!");
							if(!message.containsKey("resultSize")){
								clientOutput.writeUTF(message.toJSONString());
								clientOutput.flush();
							}else{
								hitCount = Integer.parseInt(message.get("resultSize").toString());
								int temp = sub.relayHitCounter;
								temp = temp+ hitCount;
								sub.relayHitCounter = temp;
								//relayHitCounter = relayHitCounter+hitCount;
//								System.out.println(message.toJSONString()+"  "+sub.relayHitCounter+"  ");
								sub.isReceiveOther = 1;
								//Thread.currentThread().notifyAll();
								
								Thread.yield();
								break;
							}
						}
					}
					
					//input = 0 go back to the while(ture) loop
				}
				
			
				
			
			} catch (Exception e) {
				e.printStackTrace();
			}
	

		
	}
}
