package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;



public class TaskController {
    private static Logger log = LogManager.getLogger();
    private CloseableHttpClient client = null;
    private HttpClientBuilder builder;
    private String server;
    private int retryDelay = 5 * 1000;
    private int retryCount = 2;
    private int metadataTimeout = 30 * 1000;
    public static String QUEUE_NAME = "crawler";


    ConnectionFactory factory;

    public TaskController(String _server) {
        CookieStore httpCookieStore = new BasicCookieStore();
        builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);
        client = builder.build();
        this.server = _server;

        // Настройка Rabbit
        factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
    }

    public Document getUrl(String url) {
        //String url = server + "/news/" + newsId;
        int code = 0;
        boolean bStop = false;
        Document doc = null;
        for (int iTry = 0; iTry < retryCount && !bStop; iTry++) {
//            log.info("Getting page from url: " + url);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(metadataTimeout)
                    .setConnectTimeout(metadataTimeout)
                    .setConnectionRequestTimeout(metadataTimeout)
                    .setExpectContinueEnabled(true)
                    .build();
            HttpGet request = new HttpGet(url);
            request.setConfig(requestConfig);
            CloseableHttpResponse response = null;
            try {
                response = client.execute(request);
                code = response.getStatusLine().getStatusCode();
                if (code == 404) {
                    log.warn("Error get url: " + url + " code: " + code);
                    bStop = true;//break;
                } else if (code == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try {
                            doc = Jsoup.parse(entity.getContent(), "UTF-8", server);
                            break;
                        } catch (IOException e) {
                            log.error(e);
                        }
                    }
                    bStop = true;//break;
                } else {
                    //if (code == 403) {
                    log.warn("Error get url: " + url + " code: " + code);
                    response.close();
                    response = null;
                    client.close();
                    CookieStore httpCookieStore = new BasicCookieStore();
                    builder.setDefaultCookieStore(httpCookieStore);
                    client = builder.build();
                    int delay = retryDelay * 1000 * (iTry + 1);
                    log.info("wait " + delay / 1000 + " s...");
                    try {
                        Thread.sleep(delay);
                        continue;
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            } catch (IOException e) {
                log.error(e);
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        return doc;
    }

    void getLinks(Document doc) throws InterruptedException, IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        Elements news = doc.getElementsByClass("feed__item");
        for (Element element: news) {
            try {
                Element etitle = element.child(0).child(1).child(1);
                String link = etitle.attr("href");
                channel.basicPublish("", QUEUE_NAME, null, link.getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
        channel.close();
        connection.close();
    }

    public void getPage(String link) {
        Document ndoc = getUrl(link);
        Element header_service = ndoc.select("h1.content-title").first();
        if (ndoc != null && header_service != null) {
            log.info("\n" + Thread.currentThread().getName() + " Consume");
            // Заголовок публикации
            String header = header_service.text();
            log.info("Header: " + header);

            // Текст публикации
            String newsDoc = ndoc.getElementsByClass("content content--full ").select("div[class*=l-island-a] > *"). text();
            log.info("Text: " + newsDoc); //  Уточнить: Иногда бывает реклама на странице. Не всегда!

            Element head_service = ndoc.select("div.content-header__info").first(); // Шапка статьи, в которой содержится: время публикации и автор

            // Автор публикации
            String author = head_service.child(1).child(0).text();
            log.info("Author: " + author);

            // Ссылка на страницу с публикацией
            log.info("URL: " + link);

            // Время публикации
            String time = head_service.child(2).child(1).child(0).attr("title");
            log.info("Time: " + time);
        }
    }

    void produce() throws InterruptedException, IOException, TimeoutException {
        // Сбор ссылок
        log.info("\n" + Thread.currentThread().getName() + " Produce");
        log.info("Download: " + Main.site);
        getLinks(getUrl(Main.site));
    }

    void consume() throws InterruptedException, IOException, TimeoutException {
        // Обработка ссылок
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        while (true) {
            synchronized (this) {
                try {
                    if (channel.messageCount(QUEUE_NAME) == 0) continue;
                    String url = new String(channel.basicGet(QUEUE_NAME, true).getBody(), StandardCharsets.UTF_8);
                    if (url!=null)
                        getPage(url);
                    notify();
                }
                catch (IndexOutOfBoundsException e) {
                    wait();
                }
            }
        }
    }
}

