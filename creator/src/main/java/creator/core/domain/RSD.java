package creator.core.domain;

public class RSD {
	
	private Object date;
	private Double price;
	private Double direction;
	private Double wick;
	private Double volume;
	
	public RSD() {}
	
	public RSD(Object date, Double price, Double direction, Double wick, Double volume) {
		super();
		this.date = date;
		this.price = price;
		this.direction = direction;
		this.wick = wick;
		this.volume = volume;
	}
	
	public Object getDate() {
		return date;
	}
	public Double getPrice() {
		return price;
	}
	public Double getDirection() {
		return direction;
	}
	public Double getWick() {
		return wick;
	}
	public Double getVolume() {
		return volume;
	}
	
	public void setDate(Object date) {
		this.date = date;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public void setDirection(Double direction) {
		this.direction = direction;
	}
	public void setWick(Double wick) {
		this.wick = wick;
	}
	public void setVolume(Double volume) {
		this.volume = volume;
	}
	
}
