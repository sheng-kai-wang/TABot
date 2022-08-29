# cd to /workspace/TABot

# create folders for volumes
# mkdir neo4j
# cd neo4j
# mkdir SE
# cd SE
# mkdir data logs conf import
# cd ../..

# run container
docker run \
 -p 7474:7474 \
 -p 7473:7473 \
 -p 7687:7687 \
 --volume=$(pwd)/neo4j/SE/data:/data \
 --volume=$(pwd)/neo4j/SE/logs:/logs \
 --volume=$(pwd)/neo4j/SE/conf:/conf \
 --volume=$(pwd)/neo4j/SE/import:/import \
 --env NEO4J_AUTH=neo4j/root \
 --name tabot-neo4j \
 -d neo4j:4.4.9-community
