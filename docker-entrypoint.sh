#!/usr/bin/env sh

[ -z "$MODEL_PATH" ] && MODEL_PATH=/darknet/yolov3-models/modelFromCOM19/files/
cd $MODEL_PATH && python -u /darknet/darknet_video.py -s "$MQTTHOST" -p "$MQTTPORT" \
| ffmpeg -f rawvideo -pixel_format bgr24 -video_size 800x600 -framerate 25 -i - -f avi - | nc -l -k -p 8080

