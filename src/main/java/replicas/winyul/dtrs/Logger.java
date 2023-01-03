package replicas.winyul.dtrs;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Logger {
	public static final int LOG_CLIENT=0;
	public static final int LOG_SERVER=1;
	
	//Client log methods
	public static void ClientLog(String clientID,String action,String requestParameters,String response) throws IOException{
		FileWriter fw=new FileWriter(getFileName(clientID,LOG_CLIENT),true);
		PrintWriter pw=new PrintWriter(fw);
		pw.println("Date: "+ getFormattedDate()+" Client Action: " + action + " -- Request Parameters: " + requestParameters + " -- Server Response: " + response);
		
		pw.close();
	}
	
	public static void ClientLog(String clientID,String msg) throws IOException{
		FileWriter fw=new FileWriter(getFileName(clientID,LOG_CLIENT),true);
		PrintWriter pw=new PrintWriter(fw);
		pw.println("Date: "+ getFormattedDate()+"  " + msg);
		
		pw.close();
	}
	
	
	//server log methods
	public static void ServerLog(String serverID,String clientID,String requestType ,String requestParameters,String response)  throws IOException{
		
		if(clientID.equals("null")) {
			clientID=" Event Manager";
		}
		
		FileWriter fw=new FileWriter(getFileName(serverID,LOG_SERVER),true);
		PrintWriter pw=new PrintWriter(fw);
		pw.println("Date: "+ getFormattedDate() + " ClientID: " + clientID + " -- Request Type: "+ requestType + " -- Request Parameters: " + requestParameters + " -- Server Response:  " + response);  
		
		pw.close();
	}
	
	public static void ServerLog(String serverID,String msg) throws IOException {
		FileWriter fw=new FileWriter(getFileName(serverID,LOG_SERVER),true);
		PrintWriter pw=new PrintWriter(fw);
		pw.println("Date: "+ getFormattedDate()+ " "+serverID+ " " + msg);
		
		pw.close();
	}
	//delete log file method
	public static void deleteLogFile(String id) {
		String fileName=getFileName(id,LOG_CLIENT);
		File file= new File(fileName);
		file.delete();
	}
	//return the name of the txt file name based on the id of the server/client
	private static String getFileName(String id,int LogType) {
		final String dir=System.getProperty("user.dir");
		String fileName=dir;
		
		if(LogType==LOG_SERVER) {
			if(id.equalsIgnoreCase("MTL")) {
				fileName="MontrealServer.txt";
			}else if(id.equalsIgnoreCase("TOR")){
				fileName="TorontoServer.txt";
			}else if(id.equalsIgnoreCase("VAN")) {
				fileName="VancouverServer.txt";
			}
		}
		else {
			fileName= "Client_"+ id + ".txt";
		}
		return fileName;
	}
	
	// return a formatted date to display the time for all the log updates
	private static String getFormattedDate() {
		Date date= new Date();
		String strDateFormat="yyy-MM-dd hh:mm:ss a";
		DateFormat dateFormat= new SimpleDateFormat(strDateFormat);
		
		return dateFormat.format(date);
	}
}
