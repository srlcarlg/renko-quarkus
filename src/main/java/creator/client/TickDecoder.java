package creator.client;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;

public class TickDecoder implements Decoder.Text<Tick> {
	@Override
	public Tick decode(String s) throws DecodeException {
		JsonReader jsonReader = Json.createReader(new StringReader(s));
		JsonObject jsonObject = jsonReader.readObject();
		jsonReader.close();
		return new Tick(jsonObject.getString("datetime"),
				jsonObject.getString("bid"),
				jsonObject.getString("ask"));
	}
	@Override
	public boolean willDecode(String s) {
		try {
			// Check if incoming message is valid JSON
			Json.createReader(new StringReader(s)).readObject();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
