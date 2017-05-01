import org.json.simple.JSONObject;

public class FetchResult {
	Resource resource;
	JSONObject serverResponse;

	public FetchResult(){
		
	}
	public FetchResult(Resource resource, JSONObject serverResponse){
		this.resource = resource;
		this.serverResponse = serverResponse;
	}
	
	public FetchResult(JSONObject serverResponse){
		this.serverResponse = serverResponse;
	}
}
