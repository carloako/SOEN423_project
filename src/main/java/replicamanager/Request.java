package replicamanager;

import java.net.DatagramPacket;

public class Request {
	public String msg;
	public DatagramPacket dgram;
	
	public Request(String msg, DatagramPacket dgram) {
		this.msg = msg;
		this.dgram = dgram;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public DatagramPacket getDgram() {
		return dgram;
	}

	public void setDgram(DatagramPacket dgram) {
		this.dgram = dgram;
	}
}
