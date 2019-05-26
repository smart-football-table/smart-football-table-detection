# our base build image
FROM maven:3.6-jdk-8 as maven

WORKDIR /project
COPY ./java-code/pom.xml ./pom.xml
COPY ./java-code/src ./src
RUN mvn package
RUN mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=/project/target/copy-dependencies-lib


FROM nvidia/cuda
COPY --from=maven /project/target/copy-dependencies-lib /app/lib
COPY --from=maven /project/target/detection-*.jar /app/app.jar
RUN apt-get update && apt-get -y install openjdk-8-jre

# copy python-code, git ckone darknet
# COPY requirements.txt ./
# RUN pip install -r requirements.txt

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []

