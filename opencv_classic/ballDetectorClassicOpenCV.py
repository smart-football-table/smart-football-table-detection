from collections import deque
import cv2 as cv
import argparse
import imutils
import time
import os
import numpy as np
import paho.mqtt.client as mqtt

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
ap.add_argument("-m", "--mqttport", default=1883, help="sets the mqtt broker port")
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

#define framevars
frameSize = 800

pts = deque(maxlen=bufferSize)

cap = cv.VideoCapture(pathToFile)

cap.set(28, 0)

#start mqttclient
client = mqtt.Client()
client.on_connect = on_connect

client.connect("localhost", mqttport, 60)

client.loop_start()


while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()
    
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
        
    client.publish("ball/position/abs", str(position[0]) + "," + str(position[1]))

    if(position[0]==-1):
        relPointX = position[0]
        relPointY = position[1]
    else:
        relPointX = float(position[0])/frame.shape[1]
        relPointY = float(position[1])/frame.shape[0] #0=rows

    client.publish("ball/position/rel", str(relPointX) + "," + str(relPointY))
 
        
    if args["record"] is not 'empty':
        out.write(frame)
    # uncomment to see vid through test
    cv.imshow('frame', frame)

    if cv.waitKey(20) & 0xFF == ord('q'):

        break
    
# When everything done, release the capture
cap.release()
out.release()
cv.destroyAllWindows()
