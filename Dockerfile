# base image
FROM docker pull valian/docker-python-opencv-ffmpeg

COPY requirements.txt ./
RUN pip install -r requirements.txt

# add code
RUN mkdir /usr/src/sft
COPY . /usr/src/
