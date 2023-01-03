import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;



public class sequencer {
	public static final int sequencer_udp_port=62019;
	private static Map< Integer,DatagramPacket> requestQueue;
	private static int[] replicaPorts = {};
	private static int  sequenceNumber= 0;	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		listenForRequest();		
	}
	//receive request from the front end , then multicast the packet 
	private static void listenForRequest() {
			
		DatagramSocket socket=null;
		try {
			socket=new DatagramSocket(sequencer_udp_port);
			byte[] buffer=new byte[1000];
			System.out.println(" UDP Sequencer Started at port "+ socket.getLocalPort()+"......");
			while(true) {
				DatagramPacket request= new DatagramPacket(buffer,buffer.length);
				socket.receive(request);	
				Rmulticast(replicaPorts,request);
			}
		}
		catch(SocketException e) {
			System.out.println("SocketException "+ e.getMessage());
		}
		catch(IOException e) {
			System.out.println("IOException"+ e.getMessage());
		}
		finally {
			if(socket!=null) {
				socket.close();
			}
		}
	}
	
	//multicast received packet to all ports in replicaPort by basic unicast
	public static void Rmulticast(int[] ports,DatagramPacket packet){
			sequenceNumber+=1;
			for(int port:ports){
				sendUDPMessage(port,packet);
			}
			requestQueue.put(sequenceNumber,packet);
	}
	//unicast message
	private static void sendUDPMessage(int serverPort,DatagramPacket packet) {
		DatagramSocket aSocket = null;
        String result = "";
        String sentence=new String(packet.getData(),0,packet.getLength());
		String[] parts= sentence.split(";");
		String method=parts[0];
		String clientID=parts[1];
		String eventType=parts[2];
		String eventId=parts[3];
        String dataFromClient = method + ";" + clientID + ";" + eventType + ";" + eventId+ ";"+sequenceNumber ;
        
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);
  
            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            int resultValue=Integer.parseInt(result);
            while (resultValue>0) {
            	
            	request=requestQueue.get(resultValue);
            	sentence=new String(request.getData(),0,request.getLength());
            	parts= sentence.split(";");
        		method=parts[0];
        		clientID=parts[1];
        		eventType=parts[2];
        		eventId=parts[3];
        		String replyMessage = method + ";" + clientID + ";" + eventType + ";" + eventId+ ";"+sequenceNumber ;
        		message = replyMessage.getBytes();
        		DatagramPacket rereply= new DatagramPacket(message,replyMessage.length(),reply.getAddress(),reply.getPort());
        		aSocket.send(rereply);
        		aSocket.receive(reply);
                result = new String(reply.getData());
                resultValue=Integer.parseInt(result);
            }
            
            
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }        
	
}
	
}
