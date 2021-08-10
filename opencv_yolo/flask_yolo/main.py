#!/usr/bin/env python
#
# Project: Video Streaming with Flask
# Author: Log0 <im [dot] ckieric [at] gmail [dot] com>
# Date: 2014/12/21
# Website: http://www.chioka.in/
# Description:
# Modified to support streaming out with webcams, and not just raw JPEGs.
# Most of the code credits to Miguel Grinberg, except that I made a small tweak. Thanks!
# Credits: http://blog.miguelgrinberg.com/post/video-streaming-with-flask
#
# Usage:
# 1. Install Python dependencies: cv2, flask. (wish that pip install works like a charm)
# 2. Run "python main.py".
# 3. Navigate the browser to the local webpage.
from flask import Flask, render_template, Response
from camera import VideoCamera
from collections import deque
import argparse
import imutils
import time
import os
import numpy as np
import paho.mqtt.client as mqtt
import cv2
from ctypes import *
import math
import random
import customDarknet as darknet

app = Flask(__name__)

def convertBack(x, y, w, h):
    xmin = int(round(x - (w / 2)))
    xmax = int(round(x + (w / 2)))
    ymin = int(round(y - (h / 2)))
    ymax = int(round(y + (h / 2)))
    return xmin, ymin, xmax, ymax


def cvDrawBall(detection, img):
    x, y, w, h = detection[2][0],\
        detection[2][1],\
        detection[2][2],\
        detection[2][3]
    xmin, ymin, xmax, ymax = convertBack(float(x), float(y), float(w), float(h))
    pt1 = (xmin, ymin)
    pt2 = (xmax, ymax)

    cv2.rectangle(img, pt1, pt2, (0, 255, 0), 1)

    cv2.putText(img,detection[0].decode() +" [" + str(round(detection[1] * 100, 2)) + "]",(pt1[0], pt1[1] - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.5,[0, 255, 0], 2)
    return img

def getIDHighestDetection(detections):
    idOfMaxProbability = 0
    maxProbability = 0

    for index, detection in enumerate(detections):

        probability = detection[1]
        if(probability>maxProbability):
            maxProbability = probability
            idOfMaxProbability = index

    return idOfMaxProbability

def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))

@app.route('/')
def index():
    return render_template('index.html')

def gen(camera):

    #start mqttclient
    client = mqtt.Client()
    client.on_connect = on_connect

    mqttport = 1883
    client.connect("localhost", mqttport, 60)

    client.loop_start()

    pts = deque(maxlen=bufferSize)

    global metaMain, netMain, altNames
    configPath = "/home/marco/dev/alexeyab/darknet/cfg/obj.cfg"
    weightPath = "/home/marco/dev/alexeyab/darknet/backup/fourthTry/obj_130000.weights"
    metaPath = "/home/marco/dev/alexeyab/darknet/obj.data"
    if not os.path.exists(configPath):
        raise ValueError("Invalid config path `" +
                         os.path.abspath(configPath)+"`")
    if not os.path.exists(weightPath):
        raise ValueError("Invalid weight path `" +
                         os.path.abspath(weightPath)+"`")
    if not os.path.exists(metaPath):
        raise ValueError("Invalid data file path `" +
                         os.path.abspath(metaPath)+"`")
    if netMain is None:
        netMain = darknet.load_net(configPath.encode(
            "ascii"), weightPath.encode("ascii"), 0, 1)  # batch size = 1
    if metaMain is None:
        metaMain = darknet.load_meta(metaPath.encode("ascii"))
    if altNames is None:
        try:
            with open(metaPath) as metaFH:
                metaContents = metaFH.read()
                import re
                match = re.search("names *= *(.*)$", metaContents,
                                  re.IGNORECASE | re.MULTILINE)
                if match:
                    result = match.group(1)
                else:
                    result = None
                try:
                    if os.path.exists(result):
                        with open(result) as namesFH:
                            namesList = namesFH.read().strip().split("\n")
                            altNames = [x.strip() for x in namesList]
                except TypeError:
                    pass
        except Exception:
            pass

    # Create an image we reuse for each detect
    darknet_image = darknet.make_image(darknet.network_width(netMain),
                                    darknet.network_height(netMain),3)

    while True:

        prev_time = time.time()
        ret, frame_read = camera.frame.read()
        frame_rgb = cv2.cvtColor(frame_read, cv2.COLOR_BGR2RGB)

        # cut out values for the fisheye lense, dirty hack during COM
        x_start = 9
        x_end = 440
        y_start = 0
        y_end = camera.width

        frame_rgb = frame_rgb[x_start:x_end, y_start:y_end]

        frame_rgb = cv2.resize(frame_rgb, (camera.width, camera.height))


        frame_resized = cv2.resize(frame_rgb,
                                   (darknet.network_width(netMain),
                                    darknet.network_height(netMain)),
                                   interpolation=cv2.INTER_LINEAR)

        darknet.copy_image_from_bytes(darknet_image,frame_resized.tobytes())

        detections = darknet.detect_image(netMain, metaMain, darknet_image, thresh=0.05)

        position = (-1,-1)



        if not (len(detections) is 0):
            idOfDetection = getIDHighestDetection(detections)
            position = (int(detections[idOfDetection][2][0]), int(detections[idOfDetection][2][1]))
            pts.appendleft(position)
        #else:
        #    pts.append(None)

        # loop over the set of tracked points
        for i in xrange(1, len(pts)):
            # if either of the tracked points are None, ignore
            # them
            if pts[i - 1] is None or pts[i] is None:
                continue

            # otherwise, compute the thickness of the line and
            # draw the connecting lines
            thickness = int(np.sqrt(200 / float(i + 1)) * 2)
            cv2.line(frame_resized, pts[i - 1], pts[i], (0, 255, 0), thickness)

        if(position[0]==-1):
            relPointX = position[0]
            relPointY = position[1]
        else:
            relPointX = float(position[0])/frame_resized.shape[1]
            relPointY = float(position[1])/frame_resized.shape[0]

        client.publish("ball/position/rel", str(relPointX) + "," + str(relPointY))

        image = frame_resized
        if not (len(detections) is 0):
            image = cvDrawBall(detections[idOfDetection], frame_resized)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        #image = cv2.resize(image, (1145,680))
        image = cv2.resize(image, (800,525))

        if args["record"] is not 'empty':
            out.write(image)

        cv2.imshow('Demo', image)

        cv2.moveWindow("Demo", 1025,490);

        ret, jpeg = cv2.imencode('.jpg', image)
        frame = jpeg.tobytes()

        yield (b'--frame\r\n'
            b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n\r\n')


    cap.release()
    out.release()


@app.route('/video_feed')
def video_feed():
    return Response(gen(VideoCamera(pathToFile)),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    import argparse

    netMain = None
    metaMain = None
    altNames = None

    pathToFile = 0
    bufferSize = 200
    mqttport = 1883

    # construct the argument parse and parse the arguments
    ap = argparse.ArgumentParser()
    ap.add_argument("-v", "--video", default='empty', help="path to the (optional) video file")
    ap.add_argument("-b", "--buffer", type=int, default=200, help="max buffer size for lightning track")
    ap.add_argument("-i", "--camindex", default=0, type=int, help="index of camera")
    ap.add_argument("-c", "--color", default='0,0,0,0,0,0', help="color values comma seperated")
    ap.add_argument("-r", "--record", default='empty', help="switch on recording with following file name")
    ap.add_argument("-m", "--mqttport", default='1883', help="sets the mqtt broker port")
    args = vars(ap.parse_args())

    x = args["color"].split(",")
    hsvminh = int(x[0])
    hsvmins = int(x[1])
    hsvminv = int(x[2])
    hsvmaxh = int(x[3])
    hsvmaxs = int(x[4])
    hsvmaxv = int(x[5])
    colorLower = (hsvminh, hsvmins, hsvminv)
    colorUpper = (hsvmaxh, hsvmaxs, hsvmaxv)

    if args["video"] is not 'empty':
        pathToFile = args["video"]
    else:
        pathToFile = args["camindex"]

    if args["buffer"] is not 'empty':
        bufferSize = args["buffer"]

    if args["mqttport"] is not 'empty':
        mqttport = args["mqttport"]

    if args["record"] is not 'empty':
        fileName = args["record"]
        fourcc = cv2.cv.FOURCC(*'XVID')
        out = cv2.VideoWriter((str(fileName)+'.avi'),fourcc, 20.0, (800,600))

    app.run(host='0.0.0.0', debug=True)
