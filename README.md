# crawler
## Запуск контейнера
```
docker-compose -f docker-compose.rmq.yml up -d
```

## Остановка контейнера
```
docker-compose -f docker-compose.rmq.yml down
```

## Доступ к GUI RabbitMq
### Адрес в браузере
[Жмяк](http://localhost:15672)
### Логин 
```
rabbitmq
```
### Пароль
```
rabbitmq
```
![alt-текст](https://github.com/kelrilka/crawler/blob/main/rabbitmq.png "Веб интерфейс RabbitMQ")