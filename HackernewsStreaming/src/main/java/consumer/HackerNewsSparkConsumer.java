package consumer;

import com.google.gson.Gson;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.*;

import java.io.IOException;
import java.util.*;

public class HackerNewsSparkConsumer {

    public static class HackerNewsPost {
        public String by; // Author
        public int descendants; // Comment count
        public long id; // Post ID
        public List<Integer> kids; // IDs of child comments
        public int score; // Score
        public long time; // Timestamp
        public String title; // Title
        public String type; // Type of post (e.g., "story")
        public String url; // URL of the post
    }
    
    private static Admin admin;
	private static Configuration config;
	private static Connection connection;

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws InterruptedException, IOException {
        SparkConf sparkConf = new SparkConf().setAppName("HackerNewsConsumer").setMaster("local[*]");
        JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(2));

        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "hacker-news-group");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);

        Collection<String> topics = Arrays.asList("hacker_news_topic");
        
        JavaDStream<String> stream = KafkaUtils.createDirectStream(
        		streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
        ).map(record -> record.value());
        
        setupHBaseTable();
        
        stream.foreachRDD(rdd -> {
        	rdd.foreach(entry -> {
        		Gson geo = new Gson();
            	HackerNewsPost post = geo.fromJson(entry, HackerNewsPost.class);
            	
            	if(post != null){
            		try (Table table = connection.getTable(TableName.valueOf("hacker_news_posts"))) {
                        Put put = new Put(Bytes.toBytes(post.id));
                        put.addColumn(Bytes.toBytes("post"), Bytes.toBytes("author"), Bytes.toBytes(post.by));
                        put.addColumn(Bytes.toBytes("post"), Bytes.toBytes("score"), Bytes.toBytes(post.score));
                        //put.addColumn(Bytes.toBytes("post"), Bytes.toBytes("url"), Bytes.toBytes(post.url));
                        table.put(put);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            	}
        	});
        });
        
        JavaDStream<Double> scores = stream.map(entry -> {
            Gson gson = new Gson();
            HackerNewsPost post = gson.fromJson(entry, HackerNewsPost.class);
            return (double) post.score;
        });
        
        scores.foreachRDD(rdd -> {
            if (!rdd.isEmpty()) {
                double avgScore = rdd.reduce((a, b) -> a + b) / rdd.count();
                try(Table table = connection.getTable(TableName.valueOf("hacker_news_posts"))){
                	Put put = new Put(Bytes.toBytes("analytics"));
                    put.addColumn(Bytes.toBytes("analysis"), Bytes.toBytes("avg_score"), Bytes.toBytes(avgScore));
                	table.put(put);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        streamingContext.start();
        streamingContext.awaitTermination();
    }
    
    @SuppressWarnings("deprecation")
	public static void setupHBaseTable() throws IOException {
    	config = HBaseConfiguration.create();
    	connection = ConnectionFactory.createConnection(config);
    	admin = connection.getAdmin();
    	
    	try {
    		
    		TableName tableName = TableName.valueOf("hacker_news_posts");
    		@SuppressWarnings("deprecation")
			HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
    		
    		// Define column family descriptor
            @SuppressWarnings("deprecation")
			HColumnDescriptor columnDescriptor = new HColumnDescriptor("post");
            
            // Add column family to table descriptor
            tableDescriptor.addFamily(columnDescriptor);
            
            // Create table
            admin.createTable(tableDescriptor);
            
            System.out.println("Table and column family created");
    	} catch (Exception e) {
    		System.out.println("Exception creating table: " + e.getMessage());
    	}
    	 
    }
}
