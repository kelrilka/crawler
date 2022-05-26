package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public class Main {
    public static String site = "https://vc.ru/popular";
    private static Logger log = LogManager.getLogger();

    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");

        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();
        channel.queueDeclare(TaskController.QUEUE_LINK, false, false, false, null); // Producer->Consumer
        channel.queueDeclare(TaskController.QUEUE_DB, false, false, false, null);   // Consumer->Elasticsearch
        channel.close();
        connection.close();


        // Object of a class that has both produce()
        // and consume() methods
        TaskController taskController = new TaskController(site);

        // Create producer thread
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    // Генератор ссылок
                    taskController.produce();
                }
                catch (InterruptedException | IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });

        // Create consumer thread
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    // Потребитель ссылок
                    taskController.consume();
                }
                catch (InterruptedException | IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });

        // Create thread: transmit to Elasticsearch
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    // Загрузка в БД
                    taskController.transmit();
                }
                catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        });

        // Create thread: receive to Elasticsearch (request)
        Thread t4 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    // Запрос в БД
                    taskController.receive();
                }
                catch (ExecutionException | InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Analysis: MinHash
        Thread t5 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    taskController.analysisMinHash();
                }
                catch (IOException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // Start both threads
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();

        // t2 finishes before t1
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();

        return;
    }
}


