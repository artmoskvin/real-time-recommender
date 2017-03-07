FROM java:8

COPY target/recommender-processor-assembly-1.0.jar /opt/
WORKDIR /opt
EXPOSE 8090
CMD ["java", "-cp", "recommender-processor-assembly-1.0.jar", "WebServer"]