package person.wangchen11.tcp.forward.replacer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig.ReplaceItem;

public class RawStreamReplacer extends BaseStreamReplacer {
	public RawStreamReplacer(ReplaceItem[] replaceList, InputStream in, OutputStream out) {
		super(replaceList, in, out);
	}
	
	@Override
	public void run() {
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			while(true) {
				int readLen = in.read(buffer);
				System.out.println("read : " + readLen);
				if(readLen <= 0) {
					break;
				}
				
				if(replaceList == null || replaceList.length == 0) {
					//System.out.println("write byte array");
					out.write(buffer, 0, readLen);
				} else {
					//System.out.println("#### before:\n" + new String(buffer, 0 , readLen));
					byte[] replacedBuffer = applyReplacer(buffer, 0 , readLen);
					//System.out.println("#### after:\n" + new String(replacedBuffer));
					
					out.write(replacedBuffer, 0, replacedBuffer.length);
				}
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
