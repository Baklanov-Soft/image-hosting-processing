FROM eclipse-temurin:17.0.6_10-jre-jammy

WORKDIR /opt/app

COPY ./target/scala-2.13/image-hosting-processing-recognizer-assembly-0.1.0-SNAPSHOT.jar ./
COPY synset.txt ./
COPY nsfw_model.pt ./

ENTRYPOINT ["java", "-cp", "image-hosting-processing-recognizer-assembly-0.1.0-SNAPSHOT.jar", "com.github.baklanovsoft.imagehosting.recognizer.Main"]
