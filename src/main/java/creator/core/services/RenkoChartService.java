package creator.core.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import creator.core.domain.OHLCV;
import creator.core.domain.RenkoWS;
import creator.core.domain.TopicMsg;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RenkoChartService {

	private Map<String, RenkoWS> renkosWS = new ConcurrentHashMap<>();
	private Map<String, OHLCV> prevMsg = new ConcurrentHashMap<>();

	@Channel("symbols-in-memory")
	@Broadcast
	Emitter<TopicMsg> inMemoryEmitter;

	public void initRenko(String symbol, Object date, Double price, Double brickSize) {
		renkosWS.computeIfAbsent(symbol, s -> new RenkoWS(date, price, brickSize));
		prevMsg.computeIfAbsent(symbol, s -> new OHLCV(date, price));
	}

	public void addPrices(String symbol, Object date, Double price) {
		if (renkosWS.containsKey(symbol)) {
			renkosWS.get(symbol).addPrices(date, price);
		}
	}

	public void makeChart(String symbol, String mode) {
		List<OHLCV> renko = renkosWS.get(symbol).renkoAnimate(mode);
		OHLCV ohlcv = renko.get(0);
		if (!prevMsg.get(symbol).equals(ohlcv)) {
			TopicMsg msg = new TopicMsg(symbol, ohlcv);

			OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
					.withTopic("symbol-in-memory").build();

			inMemoryEmitter.send(Message.of(msg).addMetadata(metadata));
			// quoteRequestEmitter.send(msg);
			System.out.println(symbol + ": " + ohlcv);
		}
		prevMsg.replace(symbol, ohlcv);
	}
}
