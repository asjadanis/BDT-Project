# Hacker News Streaming with Kafka, Spark, and HBase

## Overview

This project is designed to stream live data from the Hacker News API into a real-time analytics pipeline. We use Kafka as the messaging system to collect data, Spark Streaming for real-time processing, and HBase for storage.

## Technologies Used

- **Kafka**: Message broker for handling real-time data feeds.
- **Spark Streaming**: Real-time data processing.
- **HBase**: NoSQL database for storing processed data.
- **Java**: Main programming language.

## How it Works

1. **Kafka Producer**: Fetches live data from the Hacker News API and pushes it into a Kafka topic.

2. **Spark Streaming Consumer**: Consumes the data from the Kafka topic, processes it in real-time.

3. **HBase**: Spark Streaming writes the processed data into an HBase table for later analytics or querying.

## Quick Start

1. **Setup Kafka**

   Download Kafka from [Link](https://kafka.apache.org/downloads)

1. **Start Zookeeper and Kafka**

   ```bash
   bin/zookeeper-server-start.sh config/zookeeper.properties
   ```

   ```bash
   bin/kafka-server-start.sh config/server.properties
   ```

1. **Create Kafka Topic**

   ```bash
   bin/kafka-topics.sh --create --topic hacker_news_topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
   ```

1. **Run Hbase**

   ```bash
   sudo service hbase-master start
   sudo service hbase-regionserver start
   ```

## Note

Make sure all dependencies are correctly installed and all services (Kafka, Zookeeper, HBase, and Spark) are running before executing any component.
