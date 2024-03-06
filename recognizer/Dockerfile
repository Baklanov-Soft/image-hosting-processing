FROM eclipse-temurin:17-jre-jammy

WORKDIR /opt/app

COPY ./target/scala-2.13/image-hosting-processing-recognizer-assembly-0.1.0-SNAPSHOT.jar ./app.jar

# subfolder to mount nsfw model and synset here
RUN mkdir /opt/app/nsfw

ENTRYPOINT ["java", "-cp", "app.jar", "com.github.baklanovsoft.imagehosting.recognizer.Main"]
