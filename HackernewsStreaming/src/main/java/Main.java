import consumer.HackerNewsSparkConsumer;
import producer.HackerNewsProducer;


public class Main {
	public static void main(String[] args) {
        Thread producerThread = new Thread(() -> {
            try {
                HackerNewsProducer.main(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread consumerThread = new Thread(() -> {
            try {
                HackerNewsSparkConsumer.main(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        producerThread.start();
        consumerThread.start();
    }
}
