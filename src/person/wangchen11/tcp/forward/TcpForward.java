package person.wangchen11.tcp.forward;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig;

public class TcpForward extends Thread {
	private TcpForwardReplacerConfig.ForwardConfig config;
	private ServerSocket serverSocker = null;
	
	public TcpForward(TcpForwardReplacerConfig.ForwardConfig config) {
		this.config = config;
	}
	
	@Override
	public synchronized void start() {
		try {
			serverSocker = new ServerSocket(config.localPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String status = isSuccess() ? "success" : "failed";
		System.out.println("forward localhost:" + config.localPort + " -> " + config.remoteHost + ":" + config.remotePort + " " + status + " as " + (config.isHttp ? "HTTP" : "RAW"));
		
		super.start();
	}
	
	public boolean isSuccess() {
		return serverSocker != null;
	}
	
	@Override
	public void run() {
		if(!isSuccess()) {
			return;
		}

		try {
			while(true) {
				Socket socket = serverSocker.accept();
				new SocketForward(config, socket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
