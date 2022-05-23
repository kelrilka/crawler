cd Elasticsearch/docker-elk
docker-compose -f docker-compose.yml down
cd ../../RabbitMQ
docker-compose -f docker-compose.rmq.yml down