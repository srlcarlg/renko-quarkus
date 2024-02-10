package creator.core.domain;

import java.util.Objects;

public class OHLCV {
	
	private Object datetime;
	private Double open;
	private Double high;
	private Double low;
	private Double close;
	private Double volume;
	
	public OHLCV() {}
	public OHLCV(Object datetime, Double close) {
		super();
		this.datetime = datetime;
		this.open = close;
		this.high = close;
		this.low = close;
		this.close = close;
		this.volume = 1D;
	}
	public OHLCV(Object datetime, Double close, Double volume) {
		super();
		this.datetime = datetime;
		this.open = close;
		this.high = close;
		this.low = close;
		this.close = close;
		this.volume = volume;
	}
	public OHLCV(Object datetime, Double open, Double high, Double low, Double close, Double volume) {
		super();
		this.datetime = datetime;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
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
	
	@Override
	public int hashCode() {
		return Objects.hash(close, datetime, high, low, open, volume);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OHLCV other = (OHLCV) obj;
		return Objects.equals(close, other.close) && Objects.equals(high, other.high) && Objects.equals(low, other.low)
				&& Objects.equals(open, other.open) && Objects.equals(volume, other.volume);
	}
	@Override
	public String toString() {
		return "OHLCV{open=" + open + ", high=" + high + ", low=" + low + ", close=" + close + ", volume=" + volume
				+ "}";
	}
}
