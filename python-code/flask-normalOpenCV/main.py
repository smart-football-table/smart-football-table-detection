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

@app.route('/')
def index():
    return render_template('index.html')

def gen(camera):
    while True:
        ret, frame = camera.frame.read()
        
        
        #define framevars
        frameSize = 800
        bufferSize = 200
        pts = deque(maxlen=bufferSize)
        colorLower = (20, 100, 100)
        colorUpper = (30, 255, 255)
        
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
        
        ret, jpeg = cv.imencode('.jpg', frame)
        frame = jpeg.tobytes()
        
        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(gen(VideoCamera()),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)