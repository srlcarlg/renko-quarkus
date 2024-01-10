package creator.client;

/**
 * 
 */
public class Tick {
	
	private String datetime;
	private String bid;
	private String ask;
	
	public Tick() {}
	
	public Tick(String datetime, String bid, String ask) {
		super();
		this.datetime = datetime;
		this.bid = bid;
		this.ask = ask;
	}
	
	public String getDatetime() {
		return datetime;
	}
	public String getBid() {
		return bid;
	}
	public String getAsk() {
		return ask;
	}
	
	public void setDatetime(String datetime) {
		this.datetime = datetime;
	}
	public void setBid(String bid) {
		this.bid = bid;
	}
	public void setAsk(String ask) {
		this.ask = ask;
	}

	@Override
	public String toString() {
		return "Ticks{datetime=" + datetime + ", bid=" + bid + ", ask=" + ask + "}";
	}
	
	
}
