package creator.core.services;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import creator.core.domain.OHLCV;
import creator.core.domain.RenkoWS;
import creator.core.services.wrappers.PrevMsgWrapper;
import creator.core.services.wrappers.TopicMsg;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RenkoChartService {

	private static final List<String> renkoModes = Arrays.asList("normal","wicks","nongap");
	private static Map<String, RenkoWS> renkoWSList = new ConcurrentHashMap<>();
	private static Map<String, ConcurrentLinkedDeque<PrevMsgWrapper>> prevMsgMap = new ConcurrentHashMap<>();

	@Inject @Channel("normal-in-memory")
	@Broadcast
	Emitter<TopicMsg> normalEmitter;

	@Inject @Channel("wicks-in-memory")
	@Broadcast
	Emitter<TopicMsg> wicksEmitter;

	@Inject @Channel("nongap-in-memory")
	@Broadcast
	Emitter<TopicMsg> nongapEmitter;

	public void buildRenko(String symbol, Object date, Double price, Double brickSize) {
		renkoWSList.computeIfAbsent(symbol, s -> new RenkoWS(date, price, brickSize));

		// Add 1 OHLCV for all 3 modes
		prevMsgMap.computeIfAbsent(symbol, s -> {
			ConcurrentLinkedDeque<PrevMsgWrapper> modesOhlcv = new ConcurrentLinkedDeque<>();
			OHLCV ohlcv = new OHLCV(date, price);
			renkoModes.forEach(mode -> modesOhlcv.add(new PrevMsgWrapper(mode, ohlcv)));
			return modesOhlcv;
		});
		
		addPrices(symbol, date, price);
		makeChart(symbol);
	}

	public void addPrices(String symbol, Object date, Double price) {
		if (renkoWSList.containsKey(symbol)) {
			renkoWSList.get(symbol).addPrices(date, price);
		}
	}

	public void makeChart(String symbol) {
		renkoModes.parallelStream().forEach(mode ->
			prevMsgMap.get(symbol).parallelStream().forEach(wrapper -> {
				if (wrapper.getRenkoMode().equals(mode)) {
					sendTopic(symbol, mode, wrapper.getOhlcv());
				}
			})
		);
	}

	private void sendTopic(String symbol, String mode, OHLCV prevOhlcv) {
		List<OHLCV> renko = renkoWSList.get(symbol).renkoAnimate(mode);
		OHLCV ohlcv = renko.get(0);

		// New Renko
		if (!prevOhlcv.equals(ohlcv)) {
			TopicMsg msg = new TopicMsg(symbol, ohlcv);
			OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
					.withTopic(String.format("%s_%s", symbol.toLowerCase(), mode))
					.build();
			toEmitter(mode, msg, metadata);
		}
		setPrevMsg(symbol, mode, ohlcv);
		
		// Forming Renko
		// TopicMsg msg = new TopicMsg(symbol, renko.get(1));
		// OutgoingKafkaRecordMetadata<?> metadata = OutgoingKafkaRecordMetadata.builder()
		//		.withTopic(String.format("%s_%s_%s", symbol.toLowerCase(), mode, "forming"))
		//		.build();
		// toEmitter(mode, msg, metadata);
	}
	
	private void toEmitter(String mode, TopicMsg msg, OutgoingKafkaRecordMetadata<?> metadata) {
		switch (mode) {
			case "wicks": wicksEmitter.send(Message.of(msg).addMetadata(metadata)); break;
			case "nongap": nongapEmitter.send(Message.of(msg).addMetadata(metadata)); break;
			default: normalEmitter.send(Message.of(msg).addMetadata(metadata)); break;
		}
	}

	private void setPrevMsg(String symbol, String mode, OHLCV ohlcv) {
		prevMsgMap.get(symbol).forEach(wrapper -> {
			if (wrapper.getRenkoMode().equals(mode)) {
				wrapper.setOhlcv(ohlcv);
			}
		});
	}
}