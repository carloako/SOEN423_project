package replicas.winyul.dtrs;

public class ClientObject {
	private String location;
	private String type;
	private String id;
	
	
	public ClientObject(String clientID) {
		this.id=clientID;
		this.type=typeFinder(clientID);
		this.location=locationFinder(clientID);
	}
	private String locationFinder(String clientId) {
		switch(clientId.substring(0,3).toUpperCase()) {
			case "MTL":return "MTL";
			
			case "TOR":return "TOR";
			
			default: return "Van";
		}
		
	}
	private String typeFinder(String ClientId) {
		switch(ClientId.substring(3,4)) {
			case "A": return "Admin";		
			default: return"Participant";
		}
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getID() {
		return id;
	}
	public void setID(String id) {
		this.id = id;
	}
	public String toString() {
		return this.getType() + "(" + this.getID() + " ) on " + this.getLocation()+ " Server"; 
	}
}
