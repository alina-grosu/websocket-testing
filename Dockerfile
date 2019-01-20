FROM openjdk:jdk-slim-stretch

WORKDIR the_test

#install mvn, git
RUN apt-get update && apt-get install -y maven git

ADD src sources/src
ADD pom.xml sources

WORKDIR sources

RUN mvn dependency:resolve-plugins
RUN mvn clean compile test-compile

ENTRYPOINT mvn test


