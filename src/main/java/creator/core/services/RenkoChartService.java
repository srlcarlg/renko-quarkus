package creator.core.services;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import creator.core.domain.OHLCV;
import creator.core.domain.RenkoWS;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RenkoChartService {

	private RenkoWS renkoWS;

	@Channel("quote-requests")
    Emitter<OHLCV> quoteRequestEmitter;

	private OHLCV prevMsg;

	public void initRenko(Object date, Double price, Double brickSize) {
		renkoWS = new RenkoWS(date, price, brickSize);
	}

	public void addPrices(Object date, Double price) {
		renkoWS.addPrices(date, price);
	}

	public void makeChart(String mode) {
		List<OHLCV> renko = renkoWS.renkoAnimate(mode);
		var toSend = renko.get(0);
		if (!toSend.equals(prevMsg)) {
			System.out.println(toSend);
			quoteRequestEmitter.send(toSend);
		}
		prevMsg = toSend;
	}
}
