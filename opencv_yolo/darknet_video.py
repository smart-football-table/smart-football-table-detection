from ctypes import *
import math
import random
import os
import cv2
import numpy as np
import time
import customDarknet as darknet
from collections import deque
import imutils
import argparse
import paho.mqtt.client as mqtt

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

netMain = None
metaMain = None
altNames = None

bufferSize = 200
pathToFile = 0

# construct the argument parse and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-v", "--video", default='empty', help="path to the (optional) video file")
ap.add_argument("-b", "--buffer", type=int, default=200, help="max buffer size for lightning track")
ap.add_argument("-i", "--camindex", default=0, type=int, help="index of camera")
ap.add_argument("-c", "--color", default='0,0,0,0,0,0', help="not neccessary here, but important for java processbuilder")
ap.add_argument("-r", "--record", default='empty', help="switch on recording with following file name")
args = vars(ap.parse_args())

if args["video"] is not 'empty':
    pathToFile = args["video"]
else:
    pathToFile = args["camindex"]

if args["buffer"] is not 'empty':
    bufferSize = args["buffer"]

if args["record"] is not 'empty':
    fileName = args["record"]
    fourcc = cv2.cv.FOURCC(*'XVID')
    out = cv2.VideoWriter((str(fileName)+'.avi'),fourcc, 20.0, (800,525))

pts = deque(maxlen=bufferSize)


def YOLO():

    #start mqttclient
    client = mqtt.Client()
    client.on_connect = on_connect

    mqtthost = os.getenv('MQTTHOST', 'localhost')
    mqttport = str(os.getenv('MQTTPORT', '1883'))
    client.connect(mqtthost, mqttport, 60)

    client.loop_start()

    global metaMain, netMain, altNames
    configPath = 'obj.cfg'
    weightPath = 'obj.weights'
    metaPath = 'obj.data'
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
    cap = cv2.VideoCapture(pathToFile)

    width = int(cap.get(3))  # float
    height = int(cap.get(4)) # float

    # Create an image we reuse for each detect
    darknet_image = darknet.make_image(darknet.network_width(netMain),
                                    darknet.network_height(netMain),3)

    while True:
        prev_time = time.time()
        ret, frame_read = cap.read()
        frame_rgb = cv2.cvtColor(frame_read, cv2.COLOR_BGR2RGB)

        # cut out values for the fisheye lense, dirty hack during COM
        x_start = 9
        x_end = 440
        y_start = 0
        y_end = width

        frame_rgb = frame_rgb[x_start:x_end, y_start:y_end]

        frame_rgb = cv2.resize(frame_rgb, (width, height))


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

        client.publish("ball/position/abs", str(position[0]) + "," + str(position[1]))

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

        cv2.waitKey(3)
    cap.release()
    out.release()

if __name__ == "__main__":
    YOLO()
