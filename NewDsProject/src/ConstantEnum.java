/**
 * This class defines command type-related nouns and command argument-related nouns
 * to be used in the program.
 *
 */
public class ConstantEnum {
	
	public enum CommandType {
		debug, exchange, fetch, publish, query, remove, share, response, resource,resultSize,command;
	}
	
	public enum CommandArgument {
		channel, description, host, name, owner, port, secret, servers,
		tags, uri, advertisedhostname, connectionintervallimit, exchangeinterval, 
		serect, resourceTemplate, errorMessage, relay, ezserver, serverList, resourceSize;
	}
	

}
