cd Elasticsearch/docker-elk
docker-compose -f docker-compose.yml up -d
cd ../../RabbitMQ
docker-compose -f docker-compose.rmq.yml up -d