package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class Main {
    public static String site = "https://vc.ru/popular";

    private static Logger log = LogManager.getLogger();
//    private static TaskController taskController;

//    public static Set<String> setHref = new HashSet<String>();





    public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("127.0.0.1");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/");
        connectionFactory.setUsername("rabbitmq");
        connectionFactory.setPassword("rabbitmq");

        Connection connection = connectionFactory.newConnection();

        Channel channel = connection.createChannel();
        channel.queueDeclare("crawler_queue", false, false, false, null);
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

        // Start both threads
        t1.start();
        t2.start();

        // t2 finishes before t1
        t1.join();
        t2.join();

        return;
    }


//
//    // This class has a list, producer (adds items to list
//    // and consumer (removes items).
//    public static class PC {
//
//        // Create a queue shared by producer and consumer
//        // Size of list is 20.
//        public static PriorityQueue<String> QueueLink = new PriorityQueue<String>();
//        int capacity = 20;
//
//        // Function called by producer thread
//        public void produce() throws InterruptedException
//        {
//            taskController = new TaskController(site);
//            Document doc = taskController.getUrl(site);
//
//            String title;
//            title = doc.title();
//            log.info(title);
//            setHref = getLinks(doc);
//
//            Iterator<String> iterator = setHref.iterator();
//
//            log.info("\n" + Thread.currentThread().getName() + " Produce");
//
//            while (true)
//            {
//                synchronized (this)
//                {
//                    // producer thread waits while list
//                    // is full
//                    while (QueueLink.size() == capacity)
//                        wait();
//
//                    if(iterator.hasNext())
//                    {
//                        String etem = iterator.next();
//                        QueueLink.add(etem);
//                        iterator.remove();
//                    }
//
//                    // notifies the consumer thread that
//                    // now it can start consuming
//                    notify();
////                    Thread.sleep(1000);
//                }
//            }
//        }
//
//        // Function called by consumer thread
//        public void consume() throws InterruptedException
//        {
//            while (true)
//            {
//                synchronized (this)
//                {
//                    try
//                    {
//                        // consumer thread waits while list
//                        // is empty
//                        while (QueueLink.size() == 0)
//                            wait();
//
//                        log.info("\n" + Thread.currentThread().getName() + " Consume");
//                        taskController.getPage(QueueLink.poll());
//                        // Wake up producer thread
//                        notify();
////                        Thread.sleep(1000);
//
//
//                    }
//                    catch (IndexOutOfBoundsException e)
//                    {
//                        wait();
//                    }
//                }
//            }
//        }
//    }
}








