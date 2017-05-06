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
	
	public IsSubscribe(DataInputStream in,String id) {
		this.in = in;
		this.id = id;
	}
	
	
	
	@Override
	public Boolean call() {
		try {
			while(true){
				if (in.available()>0) {
					JSONParser parser = new JSONParser();
					JSONObject message = (JSONObject) parser.parse(in.readUTF());
					if (message.get("command").toString().equals("UNSUBSCRIBE")&&
							message.get("id").toString().equals(id)) {
						System.out.println("111");
						isUnsubscribe = true;
						break;
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return isUnsubscribe;
		
	}
	
}
