from collections import deque
import cv2 as cv
import argparse
import imutils
import time
import os
import numpy as np



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

pathToFile = 0
bufferSize = 64

# construct the argument parse and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-v", "--video", default='empty', help="path to the (optional) video file")
ap.add_argument("-b", "--buffer", type=int, default=64, help="max buffer size for lightning track")
ap.add_argument("-i", "--camindex", default=0, type=int, help="index of camera")
ap.add_argument("-c", "--color", default='0,0,0,0,0,0', help="color values comma seperated")
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
elif args["camindex"] is not 'empty':
    pathToFile = args["camindex"]
    
if args["buffer"] is not 'empty':
    bufferSize = args["buffer"]

frameSize = 800

pts = deque(maxlen=bufferSize)

pts.appendleft((0, 0, time.time()))
pts.appendleft((0, 0, time.time()))

cap = cv.VideoCapture(pathToFile)

cap.set(28, 0)

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()

    mask, frame = prepareFrame(colorLower, colorUpper, frameSize, cv, frame)
   
    cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
    center = None
 
    if len(cnts) > 0:
        c = max(cnts, key=cv.contourArea)
        ((x, y), radius) = cv.minEnclosingCircle(c)
        M = cv.moments(c)
        center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]), time.time())
 
        if radius > 1:
            drawDetection(int, x, cv, frame, center, y, radius)
 
    if(center == None):
        center = (-1, -1, time.time())
    pts.appendleft(center)
 
    if pts[0] is None or pts[1] is None:
        actualPointX, actualPointY = 0, 0
        previousPointX, previousPointY = 0, 0
    else:
        actualPointX = pts[1][0]
        actualPointY = pts[1][1]
        previousPointX = pts[0][0]
        previousPointY = pts[0][1]
 
    print(str(time.time()) + "|" + str(actualPointX) + "|" + str(actualPointY))
 
    # uncomment to see vid through test
    cv.imshow('frame', frame)

    if cv.waitKey(20) & 0xFF == ord('q'):

        break

# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
