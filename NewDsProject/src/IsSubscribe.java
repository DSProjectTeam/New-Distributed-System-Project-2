import java.io.DataInputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**this class is used to monitor if received unsubscribe command */

public class IsSubscribe implements Callable<Boolean>{
	DataInputStream in;
	String id;
	boolean isUnsubscribe = false;
	boolean hasDebugOption;
	boolean isSecurePort;
	
	public IsSubscribe(DataInputStream in,String id,boolean hasDebugOption, boolean isSecurePort) {
		this.in = in;
		this.id = id;
		this.hasDebugOption = hasDebugOption;
		this.isSecurePort = isSecurePort;
	}
	
	
	
	@Override
	public Boolean call() {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		try {
			while(true){
				if(!isSecurePort){
					if (in.available()>0) {
						JSONParser parser = new JSONParser();
						JSONObject message = (JSONObject) parser.parse(in.readUTF());
						if (message.get("command").toString().equals("UNSUBSCRIBE")&&
								message.get("id").toString().equals(id)) {
							if(hasDebugOption){
							       System.out.println("RECEIVED: "+message.toJSONString());
								}
							isUnsubscribe = true;
							break;
						}
					}
				}else{
					try{
						JSONParser parser = new JSONParser();
						JSONObject message = (JSONObject) parser.parse(in.readUTF());
						if (message.get("command").toString().equals("UNSUBSCRIBE")&&
								message.get("id").toString().equals(id)) {
							if(hasDebugOption){
							       System.out.println("RECEIVED: "+message.toJSONString());
								}
							isUnsubscribe = true;
							break;
						}
					}
					catch(SocketTimeoutException e){
						//should NOT be any break here.
					}
				}

			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return isUnsubscribe;
		
	}
	
}
