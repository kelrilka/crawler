# crawler
##Запуск контейнера
```zsh
docker-compose -f docker-compose.rmq.yml up -d
```

##Остановка контейнера
```zsh
docker-compose -f docker-compose.rmq.yml down
```

##Доступ к GUI RabbitMq
###Адрес в браузере
[Жмяк](http://localhost:15672)
###Логин 
```
rabbitmq
```
###Пароль
```
rabbitmq
```
/Users/kirill/Documents/МГТУ/8 семестр/ИПиАД/crawler/rabbitmq.png
![alt-текст](https://github.com/kelrilka/crawler/rabbitmq.png "Веб интерфейс RabbitMQ")