import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;

public class ServerReplica {
	
	
	/*
	 * NEED to change replica UDP port 
	 * 
	 * 
	 * List of received messages from sequencer
	 * Replica manager sequence number
	 */
	public static final int replica_udp_port=62019;
	private  Map<Integer,DatagramPacket> received;
	private int sequence =0;
	
	//listen for udp messages from sequencer
			private void listenForRequest() {
				
				DatagramSocket socket=null;
				
				try {
					socket=new DatagramSocket(replica_udp_port);
					byte[] buffer=new byte[1000];
					System.out.println(" UDP Sequencer Started at port "+ socket.getLocalPort()+"......");
					
					
					while(true) {
						DatagramPacket request= new DatagramPacket(buffer,buffer.length);
						socket.receive(request);	
						String sentence=new String(request.getData(),0,request.getLength());
						String[] parts= sentence.split(";");
						int sequenceNumber=Integer.parseInt(parts[4]);
						received.put(sequenceNumber, request);
						
						//reply the missing sequence number if missing else reply -1
						if(!received.containsKey(sequence+1)) {
							String replyMessage=sequence+1+"";
							byte[] message = replyMessage.getBytes();
							DatagramPacket reply= new DatagramPacket(message,replyMessage.length(),request.getAddress(),request.getPort());
							socket.send(reply);
						}
						else {
							String replyMessage=-1+"";
							byte[] message = replyMessage.getBytes();
							DatagramPacket reply= new DatagramPacket(message,replyMessage.length(),request.getAddress(),request.getPort());
							socket.send(reply);
							sequence+=1;
							
							DatagramPacket data=received.get(sequence);
							sentence=new String(data.getData(),0,data.getLength());
							parts= sentence.split(";");
							String method=parts[0];
							String clientID=parts[1];
							String eventType=parts[2];
							String eventId=parts[3];
							/*
							 *use data to call servers in replica manager 
							 */
						}
						
						
					}
				}
				catch(SocketException e) {
					System.out.println("SocketException "+ e.getMessage());
				}
				catch(IOException e) {
					System.out.println("IOException"+ e.getMessage());
				}
				finally {
					if(socket!=null)
						socket.close();
				}
			}
			
 }

