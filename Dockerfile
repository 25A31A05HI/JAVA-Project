FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY GiftPlanner.java .

RUN javac --add-modules jdk.httpserver GiftPlanner.java

EXPOSE 8000

CMD ["sh", "-c", "java --add-modules jdk.httpserver -Dfile.encoding=UTF-8 GiftPlanner"]
