package debugcounter;


import java.io.IOException;

import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;

public class MaxTemperatureMapper
  extends Mapper<LongWritable, Text, Text, IntWritable> {
	
	private NcdcRecordParser parser = new NcdcRecordParser();
	
	enum Temperature {
		OVER_30, OVER_100
	}
  @Override
  public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {
    
	parser.parse(value);
	if (parser.isValidTemperature()) {
		int airTemp = parser.getAirTemperature();
		if (airTemp > 300) {
			context.getCounter(Temperature.OVER_30).increment(1);
		} else if (airTemp > 1000) {
			System.err.println("Temperature over 100 degrees for input: " + value);
			context.setStatus("Detected possible corrupt record. See logs.");
			context.getCounter(Temperature.OVER_100).increment(1);
		}
	    context.write(new Text(parser.getYear()),
	          new IntWritable(parser.getAirTemperature()));
	}
  }
}
