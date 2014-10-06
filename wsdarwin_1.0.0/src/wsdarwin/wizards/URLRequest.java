package wsdarwin.wizards;

public class URLRequest {
	
	private String id;
	private String method;
	private String url;

	public URLRequest(String id, String method, String url) {
		this.id = id;
		this.method = method;
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String toString() {
		return id+" "+method+" "+url;
	}

}
