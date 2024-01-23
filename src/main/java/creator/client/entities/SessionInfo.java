package creator.client.entities;

public class SessionInfo {
	
	private String symbol;
	private Double brickSize;
	
	public SessionInfo(String symbol, Double brickSize) {
		super();
		this.symbol = symbol;
		this.brickSize = brickSize;
	}
	
	public String getSymbol() {
		return symbol;
	}
	public Double getBrickSize() {
		return brickSize;
	}
	
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public void setBrickSize(Double brickSize) {
		this.brickSize = brickSize;
	}
		
}
