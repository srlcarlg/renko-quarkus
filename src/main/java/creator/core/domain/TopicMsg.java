package creator.core.domain;

public class TopicMsg {
	
	private String symbol;
	private Object datetime;
	private Double open;
	private Double high;
	private Double low;
	private Double close;
	private Double volume;
	
	public TopicMsg(String symbol, OHLCV ohlcv) {
		super();
		this.symbol = symbol;
		if (ohlcv != null) {
			this.datetime = ohlcv.getDatetime();
			this.open = ohlcv.getOpen();
			this.high = ohlcv.getHigh();
			this.low = ohlcv.getLow();
			this.close = ohlcv.getClose();
			this.volume = ohlcv.getVolume();
		}
	}
	
	public OHLCV toOHLCV() {
		return new OHLCV(datetime, open, high, low, close, volume);
	}
	
	public String getSymbol() {
		return symbol;
	}
	public Object getDatetime() {
		return datetime;
	}
	public Double getOpen() {
		return open;
	}
	public Double getHigh() {
		return high;
	}
	public Double getLow() {
		return low;
	}
	public Double getClose() {
		return close;
	}
	public Double getVolume() {
		return volume;
	}
	
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public void setDatetime(Object datetime) {
		this.datetime = datetime;
	}
	public void setOpen(Double open) {
		this.open = open;
	}
	public void setHigh(Double high) {
		this.high = high;
	}
	public void setLow(Double low) {
		this.low = low;
	}
	public void setClose(Double close) {
		this.close = close;
	}
	public void setVolume(Double volume) {
		this.volume = volume;
	}	
}
