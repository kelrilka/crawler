# crawler

Для работы с докером в системе должен быть установлен docker и docker-compose. Если ОС Windows установите Docker Desktop.
данные RMQ будут храниться в папке 
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
