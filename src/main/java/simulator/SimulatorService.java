package simulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;

import com.exasol.parquetio.data.Row;
import com.exasol.parquetio.reader.RowParquetReader;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimulatorService {
	
	static final String RESOURCE_PATH = new File("src/main/resources").getAbsolutePath() + "/";
	static final String FILE_STRING = "_T1_cT.parquet";
	
	public List<Tick> getTickList(String symbol) {
		List<Tick> rawTicks = new ArrayList<>();
		
		final Path path = new Path(RESOURCE_PATH + symbol + FILE_STRING);
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();
	        while (row != null) {
	            Row loopRow = row;
	            rawTicks.add(
        		new Tick(loopRow.getValue("datetime").toString(),
            		loopRow.getValue("bid").toString(),
            		loopRow.getValue("ask").toString())
        		);
	            row = reader.read();
	        }
	        return rawTicks;
		} catch (final IOException exception) { return Collections.emptyList(); }
	}
}
