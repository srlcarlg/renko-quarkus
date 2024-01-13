package simulator;

import jakarta.json.Json;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class TickEncoder implements Encoder.Text<Tick> {
	@Override
	public void init(EndpointConfig ec) { }
	@Override
	public void destroy() { }
	@Override
	public String encode(Tick object) throws EncodeException {
		return Json.createObjectBuilder()
			    .add("datetime", object.getDatetime())
			    .add("bid", object.getBid())
			    .add("ask", object.getAsk())
			    .build()
			    .toString();
	}

}
