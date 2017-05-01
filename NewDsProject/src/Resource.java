import java.awt.List;
import java.io.File;
import java.util.ArrayList;

/**
 * This class defines the template of resource to be used by other classes.
 *
 */
public class Resource {
	
	/**optional user supplied name, default is " " */
	String name;
	
	/**optional user supplied description, default is " "*/
	String description;
	
	/**optional user supplied list of tag, default is empty list */
	String[] tag;
	
	/**mandatory user supplied absolute URI */ 
	String URI;
	
	/**optional user supplied channel name*/
	String channel;
	
	/**optional user supplied owner name */
	String owner;
	
	/**system supplied server:port name that lists the Resource*/
	String EZserver;
	
	resourceFile file;
	
	public Resource(){};
	
	/**constructor for this class, each instance variables are "" by default
	 * except URI should be initialized*/
	public Resource(String uri){
		this.name = "";
		this.description = "";
		/*this.tag = new String[];*/
		this.URI = uri;
		this.channel = "";
		this.owner = "";
		this.EZserver = "";
		this.file = new resourceFile(uri);
	}
	
	public Resource(String name, String[] tag, String description, String uri,
			String channel, String owner){
		this.name = name;
		this.description = description;
		this.tag = tag;
		this.URI = uri;
		this.channel = channel;
		this.owner = owner;
		this.EZserver = "";
		this.file = new resourceFile(uri);
	}
	
	public void setDefaultChannel(){
		this.channel = "public";
	}
	
	public void setPrivateChannel(String channel){
		this.channel = channel;
	}
	
}
