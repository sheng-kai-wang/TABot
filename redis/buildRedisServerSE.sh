# !/bin/sh

# cd to /workspace/TABot
# created some folders for volumes
mkdir redis
cd redis
mkdir SE
cd SE
mkdir data
echo "# bind 127.0.0.1" >> redis.conf
echo "protected-mode no" >> redis.conf
echo "appendonly yes" >> redis.conf
echo "requirepass root" >> redis.conf
cd ../..

# run container
docker run \
-p 6379:6379 \
--volume=$(pwd)/redis/SE/data:/data \
--volume=$(pwd)/redis/SE/redis.conf:/usr/local/etc/redis/redis.conf \
--name tabot-redis \
-d redis:7.0.4 \
redis-server /usr/local/etc/redis/redis.conf