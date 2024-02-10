package creator.server;

import creator.core.domain.OHLCV;
import jakarta.json.Json;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

public class OHLCVEncoder implements Encoder.Text<OHLCV> {
	@Override
	public String encode(OHLCV object) throws EncodeException {
		return Json.createObjectBuilder()
			    .add("datetime", object.getDatetime().toString())
			    .add("open", object.getOpen())
			    .add("high", object.getHigh())
			    .add("low", object.getLow())
			    .add("close", object.getClose())
			    .add("volume", object.getVolume())
			    .build()
			    .toString();
	}
}
