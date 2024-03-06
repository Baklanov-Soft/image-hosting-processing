FROM eclipse-temurin:17.0.6_10-jre-jammy

WORKDIR /opt/app

COPY ./target/scala-2.13/image-hosting-processing-resizer-assembly-0.1.0-SNAPSHOT.jar ./app.jar

ENTRYPOINT ["java", "-cp", "app.jar", "com.github.baklanovsoft.imagehosting.resizer.Main"]
