package dtrs;

public class Server_starter {

	
	//instanciate all 3 servers 
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		 Runnable task1 = () -> {
	            try {
	            	new Server("MTL",args);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        };
	        Runnable task2 = () -> {
	            try {
	            	new Server("TOR",args);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        };
	        Runnable task3 = () -> {
	            try {
	            	new Server("VAN",args);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        };
	        Thread thread1 = new Thread(task1);
	        thread1.start();
	        Thread thread2 = new Thread(task2);
	        thread2.start();
	        Thread thread3 = new Thread(task3);
	        thread3.start();
		
		
		
		
		
	}

}
