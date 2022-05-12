# crawler

## Назначение потоков
t1 – Генератор ссылок

t2 – Парсинг ссылок

t3 – Запись в БД Elasticsearch

t4 – Запрос к БД Elasticsearch

## Docker
Для работы с докером в системе должен быть установлен docker и docker-compose. Если ОС Windows, то установите Docker Desktop.
Данные RMQ будут храниться в папке:
```
./docker-data/rabbitmq
```
## Запуск контейнера
```
docker-compose -f docker-compose.rmq.yml up -d
```

## Остановка контейнера
```
docker-compose -f docker-compose.rmq.yml down
```

## Доступ к GUI RabbitMQ
### Адрес в браузере
[Жмяк](http://localhost:15672)
```
http://localhost:15672
```

### Логин 
```
rabbitmq
```
### Пароль
```
rabbitmq
```
![alt-текст](https://github.com/kelrilka/crawler/blob/main/rabbitmq.png "Веб интерфейс RabbitMQ")
![alt-текст](https://github.com/kelrilka/crawler/blob/main/example.png "Пример рабоы программы")

## Доступ к GUI Elasticsearch
Перед запуском контейнера Elasticsearch в некоторых случаях требуется наличие активного VPN.
### Адрес в браузере
[Жмяк](http://localhost:5601)
```
http://localhost:5601
```
### Запрос к БД через GUI
```
GET crawler/_search
{
    "size": 1000,
    "query": {
        "match_all": {}
    }
}
```

![alt-текст](https://github.com/kelrilka/crawler/blob/main/elasticsearch.png "elasticsearch")
