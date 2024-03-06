#docker buildx build --platform linux/amd64 -t test/recognizer .

cd ..

sbt buildRecognizer

cd recognizer

docker build -t test/recognizer .

docker image ls | grep test/recognizer
