package person.wangchen11.tcp.forward.replacer.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import person.wangchen11.tcp.forward.model.TcpForwardReplacerConfig.ReplaceItem;
import person.wangchen11.tcp.forward.replacer.BaseStreamReplacer;

public abstract class BaseHttpStreamReplacer extends BaseStreamReplacer {
	protected Map<String, String> appendHeader;
	
	public BaseHttpStreamReplacer(ReplaceItem[] replaceList, Map<String, String> appendHeader, InputStream in, OutputStream out) {
		super(replaceList, in, out);
		this.appendHeader = appendHeader;
	}
	
	public HttpInfo.HttpHeadInfo applyAppendHeader(HttpInfo.HttpHeadInfo from) {
		//System.out.println("applyAppendHeader");
		for(Entry<String, String> entry : appendHeader.entrySet()) {
			//System.out.println("applyAppendHeader: " + entry.getKey() + " -> " + entry.getValue());
			from.header.put(new CaseInsensitiveString(entry.getKey()), entry.getValue());
		}
		return from;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			HttpInfo info = new HttpInfo();
			while(true) {
				int readLen = in.read(buffer);
				//System.out.println("read : " + readLen);
				if(readLen <= 0) {
					break;
				}
				info.out.write(buffer, 0, readLen);
				info.checkStatus();
				if(info.isHeadComplete()) {
					if(info.head.getContentLength()<0 && !info.head.isChunkedTransferEncoding()) {
						info.head = applyAppendHeader(info.head);
						out.write(info.head.toString().getBytes());
						System.out.println("### write head:\n" + info.head.toString());

						int contentStart      = 0;
						byte[] allBuffer      = info.out.toByteArray();
						byte[] replacedBuffer = applyReplacer(allBuffer, contentStart, allBuffer.length - contentStart);
						
						if(isRequest()) {
							info = new HttpInfo();
							info.out.write(replacedBuffer, 0, replacedBuffer.length);
							continue;
						}
						
						out.write(replacedBuffer, 0, replacedBuffer.length);
						out.flush();
						
						
						while(true) {
							int newReadLen = in.read(buffer);
							if(newReadLen <= 0) {
								break;
							}
							System.out.println("newRead :("+newReadLen+") \n" + new String(buffer, 0, newReadLen));
							
							// FIXME handle keep alive and new request header.
							
							replacedBuffer = applyReplacer(buffer, 0, newReadLen);
							out.write(replacedBuffer, 0, replacedBuffer.length);
						}
						out.flush();
						break;
					} else {
						System.out.println("isContentComplete : " + info.isContentComplete());
						if(info.isContentComplete()) {
							byte[] contentBuffer = info.content;
							contentBuffer = applyReplacer(contentBuffer, 0, contentBuffer.length);
							info.head.setContentLength(contentBuffer.length);
							info.head = applyAppendHeader(info.head);
							byte[] headBuffer    = info.head.toString().getBytes();
							
							System.out.println("### write head:\n" + info.head.toString());

							out.write(headBuffer);
							out.write(contentBuffer);
							out.flush();
							System.out.println("write : " + new String("info.content, 0, info.content.length"));
							info = new HttpInfo();
							continue;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class HttpInfo {
		public ByteArrayOutputStream  out = new ByteArrayOutputStream();
		public ByteArrayOutputStream  contentBuffer = new ByteArrayOutputStream();
		public static final byte[] HTTP_HEAD_END = "\r\n\r\n".getBytes();
		public static final byte[] HTTP_LINE_END = "\r\n".getBytes();
		
		public HttpHeadInfo head = null;
		public byte[] content    = null;
		
		public void checkStatus() throws IOException {
			if (!isHeadComplete()) {
				byte[] buffer = out.toByteArray();
				int headEndIndex =  indexOf(buffer, 0, HTTP_HEAD_END);
				if (headEndIndex >= 0) {
					int headLen = headEndIndex + HTTP_HEAD_END.length;
					head = new HttpHeadInfo(buffer, 0, headLen);

					out.reset();
					out.write(buffer, headLen, buffer.length-headLen);
				}
			}

			if (isHeadComplete()) {
				if (!isContentComplete()) {
					if(head.isChunkedTransferEncoding()) {
						while(true) {
							byte[] buffer = out.toByteArray();
							int lineIndex = indexOf(buffer, 0, HTTP_LINE_END);
							if(lineIndex<0) {
								break;
							}
							String nextBlockSizeStr = new String(buffer, 0, lineIndex);
							int blockPos  = lineIndex+HTTP_LINE_END.length;
							int blockSize = 0;
							try {
								blockSize = Integer.parseInt(nextBlockSizeStr, 16);
								System.out.println("### blockSize is:" + blockSize + " blockPos:"+blockPos + " buffer.length:"+buffer.length);
							} catch (Exception e) {
								throw new IOException(e);
							}
							
							if(blockSize<=0) {
								content = contentBuffer.toByteArray();
								contentBuffer.reset();
								out.reset();
							}
							int blockEnd = blockPos + blockSize + HTTP_LINE_END.length;

							System.out.println("### check block size "+(blockEnd)+ ">" + buffer.length);
							if(blockEnd > buffer.length) {
								// block not ready
								break;
							}
							System.out.println("### write block:" + blockSize);
							
							contentBuffer.write(buffer, blockPos, blockSize);
							out.reset();
							out.write(buffer, blockEnd, buffer.length-blockEnd);
						}
						// TODO 
					} else {
						int contentLength = head.getContentLength();
						if(contentLength <= 0) {
							content = new byte[0];
						} else if(contentLength>0) {
							byte[] buffer = out.toByteArray();
							int contentStart = 0;
							int completeLen  = contentStart + contentLength;
							if(completeLen >= buffer.length) {
								 if(completeLen != buffer.length) {
									 System.err.println("completeLen(" + completeLen + ") != buffer.length(" + buffer.length+") should not happen!");
								 }
								 content = new byte[contentLength];
								 System.arraycopy(buffer, contentStart, content, 0, contentLength);
							}
						}
					}
				}
			}
		}
		
		public boolean isHeadComplete() {
			return head != null;
		}
		
		public boolean isContentComplete() {
			return content != null;
		}
		

		public static class HttpHeadInfo {
			public static final String HTTP_END_OF_LINE = "\r\n";
			public static final String HTTP_HEAD_ITEM_SPLIT = ":";
			public static final String HTTP_HEADER_KEY_CONTENT_LENGTH    = "Content-Length";
			public static final String HTTP_HEADER_KEY_TRANSFER_ENCODING = "Transfer-Encoding";
			
			public String protocol = "HTTP/1.1 200 OK";
			public Map<CaseInsensitiveString, String> header = new LinkedHashMap<CaseInsensitiveString, String>();
			private boolean chunkedTransferEncoding = false;
			
			public HttpHeadInfo(byte[] buffer, int start, int len) {
				String  headStr = new String(buffer, start, len);
				Scanner scanner = new Scanner(headStr);
				if(scanner.hasNextLine()) {
					protocol = scanner.nextLine().trim();
				}
				while(scanner.hasNextLine()) {
					String line = scanner.nextLine().trim();
					int splitIndex = line.indexOf(HTTP_HEAD_ITEM_SPLIT);
					if(splitIndex > 0) {
						String key   = line.substring(0, splitIndex).trim();
						String value = line.substring(splitIndex + 1, line.length()).trim();
						header.put(new CaseInsensitiveString(key), value);
					}
					

					String value = header.get(new CaseInsensitiveString(HTTP_HEADER_KEY_TRANSFER_ENCODING));
					chunkedTransferEncoding = "chunked".equalsIgnoreCase(value);
				}
				scanner.close();
			}
			
			public int getContentLength() {
				String value = header.get(new CaseInsensitiveString(HTTP_HEADER_KEY_CONTENT_LENGTH));
				if(value==null) {
					return -1;
				}
				try {
					return Integer.parseInt(value);
				} catch(Exception e) {
					e.printStackTrace();
				}
				return -1;
			}

			public boolean isChunkedTransferEncoding() {
				return chunkedTransferEncoding;
			}
			
			public void setContentLength(int len) {
				header.put(new CaseInsensitiveString(HTTP_HEADER_KEY_CONTENT_LENGTH), String.valueOf(len));
			}
			
			@Override
			public String toString() {
				StringBuffer sb = new StringBuffer();
				sb.append(protocol).append(HTTP_END_OF_LINE);
				
				for(Entry<CaseInsensitiveString, String> entry : header.entrySet()) {
					if (entry.getKey().equals(HTTP_HEADER_KEY_TRANSFER_ENCODING)) {
						// FIXME  HTTP_HEADER_KEY_TRANSFER_ENCODING vs HTTP_HEADER_KEY_CONTENT_LENGTH
						continue;
					}
					sb.append(entry.getKey())
						.append(HTTP_HEAD_ITEM_SPLIT)
						.append(" ")
						.append(entry.getValue())
						.append(HTTP_END_OF_LINE);
				}
				
				sb.append(HTTP_END_OF_LINE);
				return sb.toString();
			}
		}
	}
	
	public abstract boolean isRequest();
}
