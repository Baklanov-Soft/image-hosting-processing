docker buildx build --platform linux/amd64 -t test/recognizer .

docker image ls | grep test/recognizer
