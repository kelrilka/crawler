package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class Main {
    private static Logger log = LogManager.getLogger();
    private static TaskController taskController;
    private static String site = "https://vc.ru/popular";
    public static Set<String> setHref = new HashSet<String>();


    static Set<String> getLinks(Document doc) {
        Set<String> urls = new HashSet<String>();
        Elements news = doc.getElementsByClass("feed__item");
        for (Element element: news) {
            try {
                Element etitle = element.child(0).child(1).child(1);
                String link = etitle.attr("href");
                urls.add(link);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return urls;
    }



    public static void main(String[] args) throws InterruptedException
    {
        // Object of a class that has both produce()
        // and consume() methods
        final PC pc = new PC();

        // Create producer thread
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try {
                    // Генератор ссылок
                    pc.produce();
                }
                catch (InterruptedException e) {
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
                    pc.consume();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // Start both threads
        t1.start();
        t2.start();

        // t1 finishes before t2
        t1.join();
        t2.join();

        return;
    }



    // This class has a list, producer (adds items to list
    // and consumer (removes items).
    public static class PC {

        // Create a queue shared by producer and consumer
        // Size of list is 5.
        public static PriorityQueue<String> QueueLink = new PriorityQueue<String>();
        int capacity = 5;

        // Function called by producer thread
        public void produce() throws InterruptedException
        {
            taskController = new TaskController(site);
            Document doc = taskController.getUrl(site);

            String title;
            title = doc.title();
            log.info(title);
            setHref = getLinks(doc);

            Iterator<String> iterator = setHref.iterator();

            log.info("\n" + Thread.currentThread().getName() + " Produce");

            while (true) {
                synchronized (this)
                {
                    // producer thread waits while list
                    // is full
                    while (QueueLink.size() == capacity) {
                        wait();
                    }
                    if(iterator.hasNext()){
                        String etem = iterator.next();
                        QueueLink.add(etem);
                        iterator.remove();
                    }

                    // notifies the consumer thread that
                    // now it can start consuming
                    notify();
                    Thread.sleep(1000);
                }
            }
        }

        // Function called by consumer thread
        public void consume() throws InterruptedException
        {
            while (true) {
                synchronized (this)
                {
                    // consumer thread waits while list
                    // is empty
                    while (QueueLink.size() == 0)
                        wait();
                    synchronized (this) {
                        log.info("\n" + Thread.currentThread().getName() + " Consume");
                        taskController.getPage(QueueLink.poll());
                        // Wake up producer thread
                        notify();
                        Thread.sleep(1000);
                    }
                }
            }
        }
    }
}
