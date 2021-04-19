package avs.manager.demo;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

@Component("ProcessorBean")
public class ProcessorBean {
	private Timestamp timestamp;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private int seq;
	private int freq=100;
	private int current=0;
//	private int oracleArray[]=new int[freq];
//	private int bigtableArray[]=new int[freq];
//	private int endToEndArray[]=new int[freq];
	private HashMap<String, HashMap<String, Object>> carrierInfo = new HashMap<String, HashMap<String, Object>>();
	
	public String hashIt(String body) {
//    	System.out.println(body.hashCode()%3);
		return String.valueOf(Math.abs(body.hashCode() % 3));
	}

	public void checkSequence(Exchange exchange) {
		Timestamp temp = (Timestamp) exchange.getIn().getHeader("CamelGooglePubsub.PublishTime");
		if (this.timestamp == null) {
			this.timestamp = temp;
		}
		double seconds = temp.getSeconds() + temp.getNanos() * 1e-9;
		double last_message = timestamp.getSeconds() + timestamp.getNanos() * 1e-9;
		if (seconds - last_message < 0) {
			seq++;
			LOGGER.log(Level.INFO, "Total Messages out of order " + seq);

		}

	}

	public String addBody(String body) {
		return "KFGS7334\n"
				+ "DLZDDA87\n"
				+ "AVS\n"
				+"publishedTimestampMillis:1617258315790\n"
				+ "consumedTimestampMillis:1617258315792\n"
				+ "beforeOracleInsertedTimestampMillis:1617258315796\n"
				+ "oracleInsertedTimestampMillis:1617258315799\n"
				+ "bigTableInsertedTimestampMillis:1617258315810\n"
				+ "avsSubscriberpublishedTimeStampMillis:1617258315812\n"
				+ "avsPublisherConsumedTimeStampMillis:1617258315822\n"
				+ "avsMonitoringSubTimeStampMillis:1617258315842\n"
				+ "avsPublisherPublishedTimeStampMillis:1617258315834\n";
	}

	public String addTimestamp(String body) {
		Date date = new Date();
		return body + "\noutboundTimestampMillis:" + date.getTime();
	}

	public String getCarrier(String body) {
		String lines[] = body.split("\\r?\\n");
		return lines[3].substring(0, 2);
	}
	public long getMillis(String body,String metric) {
		Pattern p = Pattern.compile("(?<="+metric+":)(.*)(?=\n)");
		Matcher m = p.matcher(body);
		if (m.find()) {
			return Long.parseLong(m.group(1));
		}
		return -1;
		
	}
	public String addTimestamp(String body,Exchange exchange) {
		Date date = new Date();
		body=body+"\navsMonitoringSubTimeStampMillis:"+date.getTime();
		Timestamp nanoTimestamp = (com.google.protobuf.Timestamp) exchange.getMessage().getHeaders().get("CamelGooglePubsub.PublishTime"); 
		return body+"\navsPublisherPublishedTimeStampMillis:"+Timestamps.toMillis(nanoTimestamp);;
	}
	public void logMetrics(String body) {
		
		String carrier = getCarrier(body);
		
		long publishedTimestampMillis = getMillis(body,"publishedTimestampMillis");
		long consumedTimestampMillis = getMillis(body,"consumedTimestampMillis");
		long beforeOracleInsertedTimestampMillis = getMillis(body,"beforeOracleInsertedTimestampMillis");
		long oracleInsertedTimestampMillis =getMillis(body,"oracleInsertedTimestampMillis");
		long bigTableInsertedTimestampMillis =getMillis(body,"bigTableInsertedTimestampMillis");
		long avsSubscriberpublishedTimeStampMillis =getMillis(body,"avsSubscriberpublishedTimeStampMillis");
		long avsPublisherConsumedTimeStampMillis =getMillis(body,"avsPublisherConsumedTimeStampMillis");
		long avsMonitoringSubTimeStampMillis =getMillis(body,"avsMonitoringSubTimeStampMillis");
		long avsPublisherPublishedTimeStampMillis =getMillis(body,"avsPublisherPublishedTimeStampMillis");
		HashMap<String, Object> info = new HashMap<String, Object>();
		if (!(carrierInfo.containsKey(carrier))) {
			info.put("count",0);
			info.put("PublisherQueue",new int[freq]);
			info.put("oracleArray",new int[freq]);
			info.put("bigtableArray",new int[freq]);
			info.put("OutboundQueue",new int[freq]);
			info.put("AvsManagerProcessingTime",new int[freq]);
			info.put("AvsPublisherProcessingTime",new int[freq]);
			info.put("CustomerQueue",new int[freq]);
			info.put("PublisherToCustomer",new int[freq]);
		}
		else {
			info =carrierInfo.get(carrier);
		}
		int count=(int) info.get("count");
		
		int[] PublisherQueue = (int[]) info.get("PublisherQueue");
		PublisherQueue[count]=(int) (consumedTimestampMillis-publishedTimestampMillis);
		info.put("PublisherQueue",PublisherQueue);
		
			
			int[] oracleArray = (int[]) info.get("oracleArray");
			oracleArray[count]=(int) (oracleInsertedTimestampMillis-beforeOracleInsertedTimestampMillis);
			info.put("oracleArray",oracleArray);
			
			
			int[] bigtableArray = (int[]) info.get("bigtableArray");
			bigtableArray[count]=(int) (bigTableInsertedTimestampMillis-oracleInsertedTimestampMillis);
			info.put("bigtableArray",bigtableArray);
			
			int[] OutboundQueue = (int[]) info.get("OutboundQueue");
			OutboundQueue[count]=(int) (avsPublisherConsumedTimeStampMillis-avsSubscriberpublishedTimeStampMillis);
			info.put("OutboundQueue",OutboundQueue);
			
			
			int[] AvsManagerProcessingTime = (int[]) info.get("AvsManagerProcessingTime");
			AvsManagerProcessingTime[count]=(int) (avsSubscriberpublishedTimeStampMillis-consumedTimestampMillis);
			info.put("AvsManagerProcessingTime",AvsManagerProcessingTime);
			
			int[] AvsPublisherProcessingTime = (int[]) info.get("AvsPublisherProcessingTime");
			AvsPublisherProcessingTime[count]=(int) (avsPublisherPublishedTimeStampMillis-avsPublisherConsumedTimeStampMillis);
			info.put("AvsPublisherProcessingTime",AvsPublisherProcessingTime);
			
			int[] CustomerQueue = (int[]) info.get("CustomerQueue");
			CustomerQueue[count]=(int) (avsMonitoringSubTimeStampMillis-avsPublisherPublishedTimeStampMillis);
			info.put("CustomerQueue",CustomerQueue);
			
			int[] PublisherToCustomer = (int[]) info.get("PublisherToCustomer");
			PublisherToCustomer[count]=(int) (avsPublisherPublishedTimeStampMillis-publishedTimestampMillis);
			info.put("PublisherToCustomer",PublisherToCustomer);
			count++;
			info.put("count", count);
			carrierInfo.put(carrier, info);
			
		if (count==freq) {
			info.put("count",0);
			carrierInfo.put(carrier, info);
			LOGGER.log(Level.INFO, "Average latency is milliseconds over "+freq+" messages for "+carrier+" is : \n" +"AVS Manager Inbound Topic: "+
			Arrays.stream(PublisherQueue).average().orElse(-1)+"\nOracle Persistance time: "+
			Arrays.stream(oracleArray).average().orElse(-1)+"\nBigTable Persistance time: "+
			Arrays.stream(bigtableArray).average().orElse(-1)+"\nAVS Manager Outbound Topic Idle time: "+
			Arrays.stream(OutboundQueue).average().orElse(-1)+"\nAvsManager Processing Time(from subscriber start time to outbound publish time): "+
			Arrays.stream(AvsManagerProcessingTime).average().orElse(-1)+"\nAvsPublisher Processing Time (from customer start time to customer publish time): "+
			Arrays.stream(AvsPublisherProcessingTime).average().orElse(-1)+"\nCustomer Topic idle time : "+
			Arrays.stream(CustomerQueue).average().orElse(-1)+"\nPublisher To Customer end to end: "+
			Arrays.stream(PublisherToCustomer).average().orElse(-1)+"\nTotal Idle Time: "+
			(Arrays.stream(CustomerQueue).average().orElse(-1)+Arrays.stream(PublisherQueue).average().orElse(-1)+Arrays.stream(OutboundQueue).average().orElse(-1))
					);
			
			
		}
	}

}