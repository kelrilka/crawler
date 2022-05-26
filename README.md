# crawler
## Схема работы потоков
![alt-текст](https://github.com/kelrilka/crawler/blob/main/doc/crawler_scheme.png "crawler_scheme")

## Назначение потоков
t1 – Генератор ссылок

t2 – Парсинг ссылок

t3 – Запись в БД Elasticsearch

t4 – Запрос к БД Elasticsearch

t5 – Анализ MinHash

## Docker
Для работы с Docker в системе должен быть установлен Docker и Docker-Compose. Если ОС Windows, то установите Docker Desktop.
Данные RMQ будут храниться в папке:
```
./docker-data/rabbitmq
```
## Запуск контейнеров
```
./docker_stop.sh
```

## Остановка контейнеров
```
./docker_run.sh
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
![alt-текст](https://github.com/kelrilka/crawler/blob/main/doc/rabbitmq.png "Веб интерфейс RabbitMQ")
![alt-текст](https://github.com/kelrilka/crawler/blob/main/doc/example.png "Пример рабоы программы")

## Доступ к GUI Elasticsearch
Перед запуском контейнера Elasticsearch в некоторых случаях требуется наличие активного VPN.
### Адрес в браузере
[Жмяк](http://localhost:5601)
```
http://localhost:5601
```
### Запрос к БД через GUI
```
GET crawler_db/_search
{
    "size": 1000,
    "query": {
        "match_all": {}
    }
}
```

![alt-текст](https://github.com/kelrilka/crawler/blob/main/doc/elasticsearch.png "elasticsearch")

### Запрос к БД через код
![alt-текст](https://github.com/kelrilka/crawler/blob/main/doc/request.png "request")

