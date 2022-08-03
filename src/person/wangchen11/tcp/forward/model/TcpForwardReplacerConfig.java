package person.wangchen11.tcp.forward.model;

import com.google.gson.annotations.SerializedName;

public class TcpForwardReplacerConfig {

	@SerializedName("forward_list")
	public ForwardConfig[] forwardList;
	
	public static class ForwardConfig {
		@SerializedName("local_port")
		public int    localPort   = 0;
		
		@SerializedName("remote_host")
		public String remoteHost  = "localhost";
		
		@SerializedName("remote_port")
		public int    remotePort = 0;
		
		@SerializedName("replace_request_list")
		public ReplaceItem[] replaceRequestList;
		
		@SerializedName("replace_response_list")
		public ReplaceItem[] replaceResponseList;
	}
	
	
	public static class ReplaceItem {
		@SerializedName("from")
		public String from = null;
		
		@SerializedName("to")
		public String to   = "";
	}
}
