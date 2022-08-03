package person.wangchen11.tcp.forward.replacer.http;

public class CaseInsensitiveString {
	private String str;
	public CaseInsensitiveString(String str) {
		this.str = str;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof CaseInsensitiveString) {
			CaseInsensitiveString other = (CaseInsensitiveString) obj;
			return str.equalsIgnoreCase(other.str);
		}
		if(obj instanceof String) {
			return str.equals(obj.toString());
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return str.toUpperCase().hashCode();
	}
	
	@Override
	public String toString() {
		return str;
	}
}
