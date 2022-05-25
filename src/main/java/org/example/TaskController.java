package org.example;

import com.fasterxml.jackson.databind.ObjectWriter;
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
import org.elasticsearch.search.SearchHit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;


public class TaskController {
    private static Logger log = LogManager.getLogger();
    private CloseableHttpClient client = null;
    private HttpClientBuilder builder;
    private String server;
    private int retryDelay = 5 * 1000;
    private int retryCount = 2;
    private int metadataTimeout = 30 * 1000;
    public static String QUEUE_LINK = "crawler_link";
    public static String QUEUE_DB = "crawler_db";

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
                channel.basicPublish("", QUEUE_LINK, null, link.getBytes());
            } catch (Exception e) {
                log.error(e);
            }
        }
        channel.close();
        connection.close();
    }

    public void getPage(String link) throws InterruptedException, IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        Document ndoc = getUrl(link);
        Element header_service = ndoc.select("h1.content-title").first();
        if (ndoc != null && header_service != null) {
            log.info("\n" + Thread.currentThread().getName() + " Consume");
            // Заголовок публикации
            String header = header_service.text();
            log.info("Header: " + header);

            // Текст публикации
            String newsDoc = ndoc.getElementsByClass("content content--full ").select("div[class*=l-island-a] > *"). text();

            // Иногда бывает реклама на странице с новоситью в виде кода, функция text() ее не удаляет. Поэтому делаем так :)
            newsDoc = newsDoc.replaceAll("\\{.*?\\}","");
            newsDoc = newsDoc.replaceAll(",\"gtm\":\"\"}","");

            log.info("Text: " + newsDoc);

            Element head_service = ndoc.select("div.content-header__info").first(); // Шапка статьи, в которой содержится: время публикации и автор

            // Автор публикации
            String author = head_service.child(1).child(0).text();
            log.info("Author: " + author);

            // Ссылка на страницу с публикацией
            log.info("URL: " + link);

            // Время публикации
            String time = head_service.child(2).child(1).child(0).attr("title");
            log.info("Time: " + time);

            // Информацию, которую получили выше, отправляем в очередь (вторую), из которой уже будем записывать в Elasticsearch
            // Для этого из полученных данных собираем json
            Json json = new Json(header, newsDoc, author, link, time);
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json_complete = ow.writeValueAsString(json);

            // Оправляем в очередь
            channel.basicPublish("", QUEUE_DB, null, json_complete.getBytes());
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
                    if (channel.messageCount(QUEUE_LINK) == 0) continue;
                    String url = new String(channel.basicGet(QUEUE_LINK, true).getBody(), StandardCharsets.UTF_8);
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

    void transmit() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        while (true){
            if (channel.messageCount(QUEUE_DB) == 0) continue;
            String json = new String(channel.basicGet(QUEUE_DB, true).getBody(), StandardCharsets.UTF_8);
            Client client = new PreBuiltTransportClient(
                    Settings.builder().put("cluster.name","docker-cluster").build())
                    .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));
            String sha256hex = org.apache.commons.codec.digest.DigestUtils.sha256Hex(json);
            client.prepareIndex("crawler_db2", "_doc", sha256hex).setSource(json, XContentType.JSON).get();
        }
    }

    void receive() throws UnknownHostException, ExecutionException, InterruptedException {
        Client client = new PreBuiltTransportClient(
                Settings.builder().put("cluster.name","docker-cluster").build())
                .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("AUTHOR_count").field("AUTHOR.keyword");
        SearchSourceBuilder searchSourceBuilder2 = new SearchSourceBuilder().aggregation(aggregationBuilder);
        SearchRequest searchRequest2 = new SearchRequest().indices("crawler_db2").source(searchSourceBuilder2);
        SearchResponse searchResponse = client.search(searchRequest2).get();
        Terms terms = searchResponse.getAggregations().get("AUTHOR_count");

        for (Terms.Bucket bucket : terms.getBuckets())
            log.info("Count: " + bucket.getDocCount() + "\t\tAuthor: " + bucket.getKey());

        client.close();
    }

    void analysisMinHash() throws IOException, ExecutionException, InterruptedException {
        Client client = new PreBuiltTransportClient(
                Settings.builder().put("cluster.name","docker-cluster").build())
                .addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), 9300));

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("TEXT_count").field("TEXT.keyword");
        SearchSourceBuilder searchSourceBuilder2 = new SearchSourceBuilder().aggregation(aggregationBuilder);
        SearchRequest searchRequest2 = new SearchRequest().indices("crawler_db2").source(searchSourceBuilder2);
        SearchResponse searchResponse = client.search(searchRequest2).get();
        Iterator<SearchHit> sHits = searchResponse.getHits().iterator();

        List<String> results = new ArrayList<String>(5);

        for (int i=0; i<5 && sHits.hasNext(); i++) {
            results.add(sHits.next().getSourceAsString());
            continue;
        }
        client.close();

        for (int i=0; i<5; i++) {
            String json = results.get(i);
            ObjectMapper objectMapper = new ObjectMapper();
            Json textJson = objectMapper.readValue(json, Json.class);
            log.info("Text for MinHash: " + textJson.TEXT);
        }


//        Analysis.createFile("article0", );
//        Analysis.createFile("article1", "В моей картине мира рубль конвертируемый. Так почему же я не вижу повода для гордости?");
//        Analysis.nearDuplicates("article0", "article1");
    }
}

