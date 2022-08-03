package person.wangchen11.tcp.forward.replacer.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig.ReplaceItem;

public class HttpRequestStreamReplacer extends BaseHttpStreamReplacer {

	public HttpRequestStreamReplacer(ReplaceItem[] replaceList, Map<String, String> appendHeader, InputStream in,
			OutputStream out) {
		super(replaceList, appendHeader, in, out);
	}

	@Override
	public boolean isRequest() {
		return true;
	}
}
