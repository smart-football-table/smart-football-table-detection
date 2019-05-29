# our base build image
FROM nvidia/cuda:10.1-devel as cuda-devel

##########
# opencv #
##########
RUN apt-get update && apt-get install -y build-essential cmake git libgtk2.0-dev pkg-config libavcodec-dev libavformat-dev libswscale-dev python-dev python-numpy libtbb2 libtbb-dev libjpeg-dev libpng-dev libtiff-dev libdc1394-22-dev wget unzip
WORKDIR /
RUN wget -O/tmp/opencv-4.1.0.zip https://github.com/opencv/opencv/archive/4.1.0.zip && unzip /tmp/opencv-4.1.0.zip && rm /tmp/opencv-4.1.0.zip
WORKDIR opencv-4.1.0/build
RUN cmake -D CMAKE_BUILD_TYPE=Release -D OPENCV_GENERATE_PKGCONFIG=YES -D CMAKE_INSTALL_PREFIX=/usr/local .. && make && make install && ln -sf /usr/local/lib/pkgconfig/opencv4.pc /usr/local/lib/pkgconfig/opencv.pc

################
# yolo darknet #
################
WORKDIR /
RUN git clone https://github.com/AlexeyAB/darknet.git
WORKDIR darknet
RUN sed -i 's/GPU=0/GPU=1/;s/OPENCV=0/OPENCV=1/;s/LIBSO=0/LIBSO=1/' Makefile
RUN make





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

# copy python-code, git clone darknet
# COPY requirements.txt ./
# RUN pip install -r requirements.txt

### TODO copy opencv / yolo from cuda-devel


ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []

