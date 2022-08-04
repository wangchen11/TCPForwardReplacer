package person.wangchen11.tcp.forward.replacer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig;

public abstract class BaseStreamReplacer extends Thread {
	public static final int BUFFER_SIZE = 32*1024;
	protected TcpForwardReplacerConfig.ReplaceItem replaceList[];
	protected InputStream  in;
	protected OutputStream out;
	
	public BaseStreamReplacer(TcpForwardReplacerConfig.ReplaceItem replaceList[], InputStream in, OutputStream out) {
		this.replaceList = replaceList;
		this.in     = in;
		this.out    = out;
	}

	public byte[] applyReplacer(byte[] buffer, int pos, int len) {
		byte[] newBuffer = new byte[len];
		System.arraycopy(buffer, pos, newBuffer, 0, len);
		
		for(TcpForwardReplacerConfig.ReplaceItem item : replaceList) {
			if (item.from==null || item.from.length()<=0) {
				continue;
			}
			if (item.to==null || item.to.length()<=0) {
				continue;
			}
			newBuffer = replaceAll(newBuffer, item.from.getBytes(), item.to.getBytes());
		}
		
		return newBuffer;
	}
	
	public static byte[] replaceAll(byte[] buffer, byte[] from, byte[] to) {
		//System.out.println("replaceAll:" + new String(from) + " -> " + new String(to));
		if (from==null || from.length<=0) {
			return buffer;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int start = 0;
		while(true) {
			int at = indexOf(buffer, start, from);
			if(at >= 0) {
				//System.out.println("replace success:" + new String(from) + " -> " + new String(to));
				
				int leftLen = at - start;
				if (leftLen > 0) {
					out.write(buffer, start, leftLen);
				}
				out.write(to, 0, to.length);
				start = at + from.length;
			} else {
				int leftLen = buffer.length - start;
				if (leftLen > 0) {
					out.write(buffer, start, leftLen);
				}
				break;
			}
		}
		return out.toByteArray();
	}

	public static byte[] replaceFrist(byte[] buffer, byte[] from, byte[] to) {
		// TODO 
		return buffer;
	}
	
	public static int indexOf(byte[] buffer, int start, byte[] find) {
		for(int index = start; index<buffer.length; index++) {
			if(bufferCmp(buffer, index, find)) {
				return index;
			}
		}
		return -1;
	}
	
	public static boolean bufferCmp(byte[] buffer, int start, byte[] find) {
		if(start < 0) {
			return false;
		}
		if(start + find.length > buffer.length) {
			return false;
		}

		for(int findIndex = 0; findIndex<find.length; findIndex++) {
			int bufferIndex = start+findIndex;
			if(buffer[bufferIndex] != find[findIndex]) {
				return false;
			}
		}
		return true;
	}
}