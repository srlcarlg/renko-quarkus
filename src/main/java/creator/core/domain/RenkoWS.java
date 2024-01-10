package creator.core.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
	
/**
 *	Create real-time Renko OHLCV Data, usually over a WebSocket connection. <br>
 *	<strong>Modified version</strong> where Renko Single Data
 *	keeps <strong>only the last renko and the forming renko.</strong> <br>
 * 	<ul>
 * 		<li>
 * 			<strong>renkoAnimate</strong> 
 * 			method will return only these 2 elements in the list.
 *		</li>
 * 	</ul>
 */
public class RenkoWS {

	private List<WSRSD> rsd = new ArrayList<>();
	private Double brickSize;
	
    private Object wsDate;
    private Double wsPrice;
    private OHLCV wsInitialOHLCV;
    
	private Double wickMinInLoop;
	private Double wickMaxInLoop;
	private Double volumeInLoop;
	
	private final Logger logger = Logger.getLogger(getClass().getName());
	private static final List<String> MODE_LIST = List.of("normal","wicks", "nongap",
			"reverse-wicks", "reverse-nongap", "fake-r-wicks", "fake-r-nongap");
	/**
	 * 
	 * <h3>Usage</h3>
	 * <code>
	 * 	import renkodf.RenkoWS; <br>
	 * 	import renkodf.wrappers.OHLCV; <br> <br>
	 * 
	 * 	RenkoWS r = RenkoWS(date, price, brickSize); <br> <br>
	 * 	// At every price change <br>
	 * 	r.add_prices(date, price) <br>
 	 * 	List(OHLCV) df = r.renko_animate("wicks");
	 * </code>
	 * 
	 * @param date Object
	 * @param price Double
	 * @param brickSize Cannot be less than or equal to 0.00000...
	 */
	public RenkoWS(Object date, Double price, Double brickSize) {

		Double initialPrice = (Math.floor(price/brickSize)) * brickSize;
		// Renko Single Data
		rsd.add(new WSRSD(date, initialPrice, 0D, initialPrice, 1D));

		this.brickSize = brickSize;
		wsDate = date;
		wsPrice = price;
		wsInitialOHLCV = new OHLCV(date, price);
		
        wickMinInLoop = initialPrice;
        wickMaxInLoop = initialPrice;
        volumeInLoop = 1D;
	}
	/**
	 * Determine if there are new bricks to add according to the current price relative to the previous renko. <br>
	 * <strong> Must be called at every price change. </strong> <br>
	 * Here, the 'Renko Single Data' is constructed.
	 * 
	 * @param date Object
	 * @param price Double
	 * @return <strong>primitive boolean</strong> "true" if there is a new renko, otherwise "false";
	 */
	public boolean addPrices(Object date, Double price) {
		wsDate = date;
		wsPrice = price;
		
		wickMinInLoop = price.compareTo(wickMinInLoop) < 0 ? price : wickMinInLoop;
		wickMaxInLoop = price.compareTo(wickMaxInLoop) > 0 ? price : wickMaxInLoop;
		volumeInLoop += 1D;

		Double lastPrice = rsd.get(rsd.size()-1).getPrice();
		Double currentNumberBricks = (price - lastPrice) / brickSize;
		Double currentDirection = Math.signum(currentNumberBricks);
		if (currentDirection == 0) {
        	return false;
        }  
		Double lastDirection = rsd.get(rsd.size()-1).getDirection();
		boolean isSameDirection = (currentDirection > 0 && lastDirection >= 0) || (currentDirection < 0 && lastDirection <= 0);
        
		// CURRENT PRICE in same direction of the LAST RENKO
		Double totalSameBricks = isSameDirection ? currentNumberBricks : 0;
        /* >= 2 can be a 'GAP' or 'OPPOSITE DIRECTION'.
         * In both cases we add the current wick/volume to the first brick and 'reset' the value of both, since:
         * If it's a GAP: 
		 *	- The following bricks after first brick will be 'artificial' since the price has 'skipped' that price region.
		 *	- (the reason of 'totalSameBricks')
		 * If it's a OPPOSITE DIRECTION:
		 *	- Only the first brick will be kept. (the reason of '2' multiply) 
		 */
        if (!isSameDirection && Math.abs(currentNumberBricks) >= 2) {
        	addBrickLoop(date, 2, currentDirection, currentNumberBricks);
        	totalSameBricks =  currentNumberBricks - (2 * currentDirection);
        }
        
        // Add all bricks in the same direction
        for (int noUse = 0; noUse < Math.abs(totalSameBricks.intValue()); noUse++) {
        	addBrickLoop(date, 1, currentDirection, currentNumberBricks);
		}

        return true;
	}
	
	private void addBrickLoop(Object date, Integer renkoMultiply,  Double currentDirection, Double currentNumberBricks) {
		
		// Need update value because of 'same direction' inner loop
		Double lastPrice = rsd.get(rsd.size()-1).getPrice();
        Double renkoPrice = lastPrice + (currentDirection * renkoMultiply * brickSize);
        Double wick = currentNumberBricks > 0 ? wickMinInLoop  : wickMaxInLoop;
        
        if (rsd.size() >= 2) {
            rsd.set(rsd.size()-2, rsd.get(rsd.size()-1));
            rsd.set(rsd.size()-1, new WSRSD(date, renkoPrice, currentDirection, wick, volumeInLoop));
        }
        else {
            rsd.add(new WSRSD(date, renkoPrice, currentDirection, wick, volumeInLoop));
        }
        
        // Reset
        volumeInLoop = 1D;
        wickMinInLoop = currentNumberBricks > 0 ? renkoPrice : wickMinInLoop;
        wickMaxInLoop = currentNumberBricks < 0 ? renkoPrice : wickMaxInLoop;
	}
	/**
	 * Transforms 'Renko Single Data' into OHLCV List.
	 * @param mode
	 * @return
	 */
	private List<OHLCV> renkodf(String mode) {
		
		String msg = String.format("Mode: \"%s\" does not exist, using \"normal\" instead", mode);
		if (!MODE_LIST.contains(mode)) {
			logger.log(Level.WARNING, msg);
			mode = MODE_LIST.get(0);
		}

		List<OHLCV> renkoList = new ArrayList<>(rsd.size());

        boolean reverseModeRule = Set.of("normal","wicks","reverse-wicks","fake-r-wicks").contains(mode);
        boolean fakeReverseModeRule = Set.of("fake-r-nongap", "fake-r-wicks").contains(mode);
        boolean sameDirectionRule = Set.of("wicks","nongap").contains(mode);

        Double prevDirection = 0D;
        Double prevClose = 0D;
		Double prevCloseUP = 0D;
        Double prevCloseDOWN = 0D;
        for (int i = 0; i < rsd.size(); i++) {

        	Double price = rsd.get(i).getPrice();
        	Double direction = rsd.get(i).getDirection();
        	Object date = rsd.get(i).getDate();
        	Double wick = rsd.get(i).getWick();
        	Double volume = rsd.get(i).getVolume();
        	
            OHLCV toAdd = new OHLCV(date, price, volume);

            // Current Renko (UP)
        	if (direction == 1.0) {
        		
                toAdd.setHigh(price);
                // Previous same direction(UP)
                if (prevDirection == 1) {
                	toAdd.setOpen(mode.equals("nongap") ? wick : prevClose);
                	toAdd.setLow(sameDirectionRule ? wick : prevClose);
                }
                // Previous reverse direction(DOWN) 
                else {
                    if (reverseModeRule) {
                    	toAdd.setOpen(prevClose + brickSize);
                    } else if (mode.equals("fake-r-nongap")) {
                    	toAdd.setOpen(prevCloseDOWN);
                    } else {
                    	toAdd.setOpen(wick);
                    }
                    if (mode.equals("normal")) {
                    	toAdd.setLow(prevClose + brickSize);
                    } else if (fakeReverseModeRule) {
                    	toAdd.setLow(prevCloseDOWN);
                    } else {
                    	toAdd.setLow(wick);
                    }
        		}
                
                renkoList.add(toAdd);
                prevCloseUP = price;
        	}
            // Current Renko (DOWN)
        	else if (direction == -1) {
        		
                toAdd.setLow(price);
                // Previous same direction(DOWN)
                if (prevDirection == -1) {
                	toAdd.setOpen(mode.equals("nongap") ? wick : prevClose);
                	toAdd.setHigh(sameDirectionRule ? wick : prevClose);
                }
                // Previous reverse direction(UP) 
                else {
                    if (reverseModeRule) {
                    	toAdd.setOpen(prevClose - brickSize);
                    } else if (mode.equals("fake-r-nongap")) {
                    	toAdd.setOpen(prevCloseUP);
                    } else {
                    	toAdd.setOpen(wick);
                    }

                    if (mode.equals("normal")) {
                    	toAdd.setHigh(prevClose - brickSize);
                    } else if (fakeReverseModeRule) {
                    	toAdd.setHigh(prevCloseUP);
                    } else {
                    	toAdd.setHigh(wick);
                    }
        		}
                
                renkoList.add(toAdd);
                prevCloseDOWN = price;
        	}

            prevDirection = direction;
            prevClose = price;
		}

        // Removing first row
        if (!renkoList.isEmpty()) {
        	renkoList.remove(0);
        }

        return renkoList;
	}
	/**
	 * Should be called after 'RenkoWS.addPrices(date, price)'
	 * 
	 * @param mode The method for building the Renko List, described in the Renko.renkodf().
	 * @return List of OHLCV with Forming Renko
	 */
	public List<OHLCV> renkoAnimate(String mode) {
		
		List<OHLCV> renkodf = renkodf(mode);
		List<OHLCV> renkoList = new ArrayList<>(rsd.size() + 1);
		
		renkoList.add(new OHLCV(wsDate, wsPrice, volumeInLoop));

		if (renkodf.isEmpty()) {
			renkoList.set(0, wsInitialOHLCV);
			
			OHLCV toAdd = new OHLCV(wsDate, wsPrice, volumeInLoop);
			toAdd.setOpen(wsInitialOHLCV.getClose());
			toAdd.setHigh(wickMaxInLoop);
			toAdd.setLow(wickMinInLoop);
			
			renkoList.add(toAdd);
            return renkoList;
		}

        Integer lastIndex = renkoList.size()-1;
		// Forming wick
		renkoList.get(lastIndex).setHigh(
				!mode.equals(MODE_LIST.get(0)) ? wickMaxInLoop : wsPrice);
		renkoList.get(lastIndex).setLow(
				!mode.equals(MODE_LIST.get(0)) ? wickMinInLoop : wsPrice);
		
        boolean nongapRule = Set.of("nongap", "reverse-nongap", "fake-r-nongap").contains(mode);
        
        Double lastRenkoClose = renkodf.get(renkodf.size()-1).getClose();
        Double lastRenkoOpen = renkodf.get(renkodf.size()-1).getOpen();
        
        // Last Renko (UP)
        if (lastRenkoClose > lastRenkoOpen) {
            if (wsPrice > lastRenkoClose) {
                renkoList.get(lastIndex).setOpen(nongapRule ? wickMinInLoop : lastRenkoClose);
                if (mode.equals(MODE_LIST.get(0))) {
                    renkoList.get(lastIndex).setLow(lastRenkoClose);
                }
            }
            else {
            	if (wsPrice < lastRenkoOpen) {
                    renkoList.get(lastIndex).setOpen(nongapRule ? wickMaxInLoop : lastRenkoOpen);
                    if (mode.equals(MODE_LIST.get(0))) {
                        renkoList.get(lastIndex).setHigh(lastRenkoOpen);
                    }
            	}
            }
        }
        // Last Renko (DOWN)
        else {
            if (wsPrice < lastRenkoClose) {
                renkoList.get(lastIndex).setOpen(nongapRule ? wickMaxInLoop : lastRenkoClose);
                if (mode.equals(MODE_LIST.get(0))) {
                    renkoList.get(lastIndex).setHigh(lastRenkoClose);		
                }
            }
            else {
                if (wsPrice > lastRenkoOpen) {
                    renkoList.get(lastIndex).setOpen(nongapRule ? wickMinInLoop : lastRenkoOpen);
                    if (mode.equals(MODE_LIST.get(0))) {
                        renkoList.get(lastIndex).setLow(lastRenkoOpen);
                    }
                }
            }
        }
        
        return Stream.of(renkodf, renkoList).flatMap(List::stream).collect(Collectors.toList());
	}
}
