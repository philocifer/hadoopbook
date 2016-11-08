import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.sqoop.lib.RecordParser.ParseError;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;


public class MaxWidgetId extends Configured implements Tool {

	public static class MaxWidgetMapper extends Mapper<LongWritable, Text, LongWritable, Widget> {
		
		private Widget maxWidget = null;
		
		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			Widget widget = new Widget();
			try {
				widget.parse(value);
			} 
			catch (ParseError pe) {
				return;
			}
			
			Integer id = widget.get_id();
			if (id == null) {
				return;
			}
			else {
				if (maxWidget == null || maxWidget.get_id().intValue() < id.intValue()) {
					maxWidget = widget;
				}
			}
		}
		
		public void cleanup(Context context) throws IOException, InterruptedException {
			if (maxWidget != null) {
				context.write(new LongWritable(0), maxWidget);
			}
		}
	}
	
	public static class MaxWidgetReducer extends Reducer<LongWritable, Widget, Widget, NullWritable> {
		
		@Override
		public void reduce(LongWritable key, Iterable<Widget> values, Context context) throws IOException, InterruptedException {
			Widget maxWidget = null;
			
			for (Widget widget : values) {
				if (maxWidget == null || maxWidget.get_id().intValue() < widget.get_id().intValue()) {
					try {
						maxWidget = (Widget) widget.clone();
					} catch (CloneNotSupportedException cnse) {
						throw new IOException(cnse);
					}
				}
			}
			
			if (maxWidget != null) {
				context.write(maxWidget, NullWritable.get());
			}
		}
	}
	
	
	@Override
	public int run(String[] args) throws Exception {
		Job job = new Job(getConf(), "Max Widget Id");
		job.setJarByClass(MaxWidgetId.class);

		job.setMapperClass(MaxWidgetMapper.class);
		job.setReducerClass(MaxWidgetReducer.class);

		FileInputFormat.addInputPath(job, new Path("widgets"));
	    FileOutputFormat.setOutputPath(job, new Path("maxwidget"));

	    job.setMapOutputKeyClass(LongWritable.class);
	    job.setMapOutputValueClass(Widget.class);

	    job.setOutputKeyClass(Widget.class);
	    job.setOutputValueClass(NullWritable.class);

	    job.setNumReduceTasks(1);

	    return job.waitForCompletion(true) ? 0 : 1;
	}

	public static void main(String [] args) throws Exception {
		    int exitCode = ToolRunner.run(new MaxWidgetId(), args);
		    System.exit(exitCode);
	}
}
