import java.util.ArrayList;

import org.json.simple.JSONObject;

/**
 * This class defines the query outcome return by other server
 *
 */
public class QueryData {
	boolean hasMatch;
	ArrayList<JSONObject> outcome;
	
	/**constructor, null indicates no resource match*/
	public QueryData(){
		
	}
	
	/**constructor, return with match result*/
	public QueryData(Boolean hasMatch, ArrayList<JSONObject> outcome){
		this.hasMatch = hasMatch;
		this.outcome = outcome;
	}
	
	
}
