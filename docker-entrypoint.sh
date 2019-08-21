#!/usr/bin/env sh

[ -z "$MODEL_PATH" ] && MODEL_PATH=/darknet/yolov3-models/modelFromCOM19/files/
cd $MODEL_PATH && python -u /darknet/darknet_video.py

