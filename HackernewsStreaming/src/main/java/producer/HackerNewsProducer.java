package producer;

import org.apache.kafka.clients.producer.*;

import com.google.gson.Gson;

import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HackerNewsProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        System.out.println("Starting producer");
        
        int startIndex = 0;
        int batchSize = 100;
 
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
        	while (true) {
        		// Fetch data from Hacker News API
                List<String> hackerNewsData = fetchDataFromHackerNewsAPI(startIndex, batchSize);
                
                // Send to Kafka topic
                for (String data : hackerNewsData) {
                    System.out.println("Sending the following data to Kafka: " + data);
                    ProducerRecord<String, String> record = new ProducerRecord<>("hacker_news_topic", null, data);
                    producer.send(record);
                }
                // Update the start index for the next batch
                startIndex += batchSize;

                // Wait for 2 seconds before the next batch
                Thread.sleep(2000);
        	}
            
        } catch (Exception e) {
            System.out.println("Exception occurred while sending to Kafka: " + e.getMessage());
        }
        
    }
    
    public static List<String> fetchDataFromHackerNewsAPI(int startIndex, int batchSize) {
        List<String> topStoriesData = new ArrayList<String>();
        try {
            // Fetch top stories IDs
            String topStoriesUrl = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
            String topStoriesIds = fetchData(topStoriesUrl);

            // Convert the response to an array of IDs
            Integer[] storyIds = new Gson().fromJson(topStoriesIds, Integer[].class);
            
            // Check if we've reached the end of the story IDs
            if (startIndex >= storyIds.length) {
                startIndex = 0;  // Reset to start if needed
            }

            // Calculate end index
            int endIndex = Math.min(startIndex + batchSize, storyIds.length);


            // Fetch details for each story, limiting to first 5 for this example
            for (int i = startIndex; i < endIndex; i++) {
                String storyUrl = "https://hacker-news.firebaseio.com/v0/item/" + storyIds[i] + ".json?print=pretty";
                String storyData = fetchData(storyUrl);
                topStoriesData.add(storyData);
            }

        } catch (Exception e) {
            System.out.println("Exception occurred while fetching posts: " + e.getMessage());
        }

        return topStoriesData;
    }
    
    private static String fetchData(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }
}

