import java.io.DataInputStream;
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
	
	public IsSubscribe(DataInputStream in,String id,boolean hasDebugOption) {
		this.in = in;
		this.id = id;
		this.hasDebugOption = hasDebugOption;
	}
	
	
	
	@Override
	public Boolean call() {
		try {
			while(true){
				if (in.available()>0) {
					JSONParser parser = new JSONParser();
					JSONObject message = (JSONObject) parser.parse(in.readUTF());
					if (message.get("command").toString().equals("UNSUBSCRIBE")&&
							message.get("id").toString().equals(id)) {System.out.println(hasDebugOption);
						if(hasDebugOption){
						       System.out.println("RECEIVED: "+message.toJSONString());
							}
						isUnsubscribe = true;
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isUnsubscribe;
		
	}
	
}
