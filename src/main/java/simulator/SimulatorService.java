package simulator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimulatorService {
	static final String FILE_STRING = "_T1_cT.csv";

	public List<Tick> getTickList(String symbol) {
		List<Tick> rawTicks = new ArrayList<>();
		final URL url = this.getClass().getClassLoader().getResource(symbol + FILE_STRING);
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));) {
			String row = bufferedReader.readLine();
			// ignore the row with columns names
			row = bufferedReader.readLine();
			while (row != null) {
				// use comma as separator
				String[] loopRow = row.split(";");
				rawTicks.add(new Tick(
						loopRow[0], // datetime
						loopRow[1], // bid
						loopRow[2] // ask
				));
				row = bufferedReader.readLine();
			}
			return rawTicks;
		} catch (Exception exception) {
			System.out.println(String.format("ERROR CSVReader> %s", exception));
			return Collections.emptyList();
		}
	}
}
