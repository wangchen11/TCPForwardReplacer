package person.wangchen11.tcp.forward;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig;

public class TcpForwardReplacer {
	
	private TcpForwardReplacerConfig config;

    public static void usage() {
    	System.out.println("tcpfwd-replacer config.json");
    }
	
    public static void main(String[] args) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
    	args = new String[] { "replacer-config.json" };
    	if(args.length <= 0) {
    		usage();
    		System.exit(-1);
    		return;
    	}
    	
    	System.out.println("using config:" + args[0]);
    	
    	Gson gson = new Gson();
    	TcpForwardReplacerConfig config = gson.fromJson(new FileReader(args[0]), TcpForwardReplacerConfig.class);
    	TcpForwardReplacer replacer = new TcpForwardReplacer(config);
    	int exitValue = replacer.excute();
		System.exit(exitValue);
    }
    
    public TcpForwardReplacer(TcpForwardReplacerConfig config) {
    	this.config = config;
    }
    
    public int excute() {
    	List<TcpForward> forwardList = new ArrayList<TcpForward>();
    	
    	for(TcpForwardReplacerConfig.ForwardConfig fc : config.forwardList) {
    		forwardList.add(new TcpForward(fc));
    	}

    	for(TcpForward fwd: forwardList) {
    		fwd.start();
    	}

    	for(TcpForward fwd: forwardList) {
    		try {
				fwd.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
		return 0;
    }
    
}