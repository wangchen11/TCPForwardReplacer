package person.wangchen11.tcp.forward;

import java.io.IOException;
import java.net.Socket;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig;

public class SocketForward extends Thread {
	TcpForwardReplacerConfig.ForwardConfig config;
	private Socket from = null;
	private Socket to   = null;
	
	public SocketForward(TcpForwardReplacerConfig.ForwardConfig config, Socket from) {
		this.config = config;
		this.from   = from;
	}
	
	@Override
	public void run() {
		try {
			//System.out.println("start fowrard to " + config.remoteHost + ":" + config.remotePort);
			to = new Socket(config.remoteHost, config.remotePort);
			
			StreamReplacer request  = new StreamReplacer(config.replaceRequestList, from.getInputStream(), to.getOutputStream());
			StreamReplacer response = new StreamReplacer(config.replaceResponseList, to.getInputStream(), from.getOutputStream());
			request.start();
			response.start();
			try {
				request.join();
				response.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("end  fowrard to " + config.remoteHost + ":" + config.remotePort);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(to != null) {
			try {
				to.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(from != null) {
			try {
				from.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
