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
import cv2 as cv

app = Flask(__name__)

def drawDetection(int, x, cv, frame, center, y, radius):
    cv.circle(frame, (int(x), int(y)), int(radius), (255, 255, 255), 2)
    cv.circle(frame, (center[0], center[1]), 5, (0, 0, 255), -1)

def prepareFrame(colorLower, colorUpper, frameSize, cv, frame):
    frame = imutils.resize(frame, width=frameSize)
    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)
    mask = cv.inRange(hsv, colorLower, colorUpper)
    mask = cv.erode(mask, None, iterations=2)
    mask = cv.dilate(mask, None, iterations=2)
    return mask, frame

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
    
    while True:
        ret, frame = camera.frame.read()
        
        #define framevars
        frameSize = 800
        pts = deque(maxlen=bufferSize)
        
        mask, frame = prepareFrame(colorLower, colorUpper, frameSize, cv, frame)
   
        position = (-1,-1)
   
        cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
        
        if len(cnts) > 0:
            c = max(cnts, key=cv.contourArea)
            ((x, y), radius) = cv.minEnclosingCircle(c)
            M = cv.moments(c)
            position = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
 
            if radius > 1:
                drawDetection(int, x, cv, frame, position, y, radius)
 
        if not (position[0] == -1):
            pts.appendleft(position)
    
        # loop over the set of tracked points
        for i in xrange(1, len(pts)):
            # if either of the tracked points are None, ignore
            # them
            if pts[i - 1] is None or pts[i] is None:
                continue

            # otherwise, compute the thickness of the line and
            # draw the connecting lines
            thickness = int(np.sqrt(200 / float(i + 1)) * 2)
            cv.line(frame, pts[i - 1], pts[i], (0, 0, 255), thickness)
        
        actualPointX = position[0]
        actualPointY = position[1]
    
        client.publish("ball/position/abs", str(actualPointX) + "," + str(actualPointY))
        client.publish("ball/position/rel", str(float(actualPointX)/frame.shape[0]) + "," + str(float(actualPointY)/frame.shape[1]))
        
        ret, jpeg = cv.imencode('.jpg', frame)
        frame = jpeg.tobytes()
        
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(gen(VideoCamera(pathToFile)),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    import argparse
    
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
        fourcc = cv.cv.FOURCC(*'XVID')
        out = cv.VideoWriter((str(fileName)+'.avi'),fourcc, 20.0, (800,600))
        
    app.run(host='0.0.0.0', debug=True)