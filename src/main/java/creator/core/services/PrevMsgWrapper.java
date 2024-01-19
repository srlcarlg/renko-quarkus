package creator.core.services;

import creator.core.domain.OHLCV;

public class PrevMsgWrapper {
	
	private String renkoMode;
	private OHLCV ohlcv;	
	
	public PrevMsgWrapper(String renkoMode, OHLCV ohlcv) {
		super();
		this.renkoMode = renkoMode;
		this.ohlcv = ohlcv;
	}
	
	public String getRenkoMode() {
		return renkoMode;
	}
	public OHLCV getOhlcv() {
		return ohlcv;
	}
	
	public void setRenkoMode(String renkoMode) {
		this.renkoMode = renkoMode;
	}
	public void setOhlcv(OHLCV ohlcv) {
		this.ohlcv = ohlcv;
	}
}
