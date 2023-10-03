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

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws InterruptedException {
        SparkConf sparkConf = new SparkConf().setAppName("HackerNewsConsumer").setMaster("local[*]");
        JavaStreamingContext streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(2));

        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "localhost:9092");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "hacker-news-group");
        kafkaParams.put("auto.offset.reset", "latest");

        Collection<String> topics = Arrays.asList("hacker_news_topic");

        JavaInputDStream<ConsumerRecord<String, String>> stream =
                KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams)
                );

        JavaDStream<String> lines = stream.map(ConsumerRecord::value);

        JavaDStream<HackerNewsPost> posts = lines.map(json -> new Gson().fromJson(json, HackerNewsPost.class));

        posts.foreachRDD(rdd -> {
            rdd.foreachPartition(records -> {
                Configuration config = HBaseConfiguration.create();
                Connection connection = null;
                Admin admin = null;
                final Table[] tableContainer = new Table[1];

                try {
                    connection = ConnectionFactory.createConnection(config);
                    admin = connection.getAdmin();
                    TableName tableName = TableName.valueOf("hacker_news_posts");

                    if (!admin.tableExists(tableName)) {
                        // Define table descriptor
                        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
                        
                        // Define column family descriptor
                        HColumnDescriptor columnDescriptor = new HColumnDescriptor("post");
                        
                        // Add column family to table descriptor
                        tableDescriptor.addFamily(columnDescriptor);
                        
                        // Create table
                        admin.createTable(tableDescriptor);
                        System.out.println("Table and column family created");
                    }

                    tableContainer[0] = connection.getTable(tableName);

                    records.forEachRemaining(post -> {
                        Put put = new Put(Bytes.toBytes(post.title));
                        put.addColumn(Bytes.toBytes("post"), Bytes.toBytes("author"), Bytes.toBytes(post.by));
                        put.addColumn(Bytes.toBytes("post"), Bytes.toBytes("url"), Bytes.toBytes(post.url));
                        try {
                            tableContainer[0].put(put);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (admin != null) {
                            admin.close();
                        }
                        if (tableContainer[0] != null) {
                            tableContainer[0].close();
                        }
                        if (connection != null) {
                            connection.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });
        });

        streamingContext.start();
        streamingContext.awaitTermination();
    }
}

