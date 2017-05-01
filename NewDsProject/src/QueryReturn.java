import java.io.BufferedWriter;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.simple.JSONObject;


/**
 * This class defines the query outcome without the relay operation.
 *
 */
public class QueryReturn {
	Boolean hasMatch;
	ArrayList<JSONObject> returnList = new ArrayList<>();
	JSONObject reponseMessage;	
	
	/**constructor, return with match result*/
	public QueryReturn(ArrayList<JSONObject> returnList){
		this.returnList = returnList;
		hasMatch = true;
	}
	
	/**constructor, return error message without match result*/
	public QueryReturn(JSONObject jsonObject){
		this.reponseMessage = jsonObject;
		hasMatch = false;
	}
	
	
}
