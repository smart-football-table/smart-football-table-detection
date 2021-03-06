from collections import deque
import cv2 as cv
import argparse
import imutils
import time
import os
import numpy as np

parser = argparse.ArgumentParser()
parser.add_argument("a", nargs='?', default="empty")
args = parser.parse_args()

if args.a == 'empty':
    greenLower = (20, 100, 100)
    greenUpper = (30, 255, 255)
else:
    x = args.a.split(",")
    hsvminh = int(x[0])
    hsvmins = int(x[1])
    hsvminv = int(x[2])
    hsvmaxh = int(x[3])
    hsvmaxs = int(x[4])
    hsvmaxv = int(x[5])
    greenLower = (hsvminh, hsvmins, hsvminv)
    greenUpper = (hsvmaxh, hsvmaxs, hsvmaxv)

frameSize = 800

pts = deque(maxlen=20000)

pts.appendleft((0, 0, time.time()))
pts.appendleft((0, 0, time.time()))

cap = cv.VideoCapture("../../../Schreibtisch/testvideos/schief.avi")

cap.set(28, 0)

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()

    frame = imutils.resize(frame, width=frameSize)
    
   # gaussian = cv.adaptiveThreshold(frame,255,cv.ADAPTIVE_THRESH_GAUSSIAN_C,cv.THRESH_BINARY,11,2)
    #cv.imshow('gaussian', gaussian)

    edges = cv.Canny(frame, 100, 200)
    cv.imshow('edges', edges)
    
    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)

    cv.imshow('hsv', hsv)
    
    (channel_h, channel_s, channel_v) = cv.split(hsv)
    
    cv.imshow("h channel",channel_h);
    cv.imshow("s channel",channel_s);
    cv.imshow("v channel",channel_v);
    
    channel_s.fill(0)
    hsv_image_edited = cv.merge([channel_h, channel_s, channel_v])
    cv.imshow("h+s+v after",hsv_image_edited);

    mask = cv.inRange(hsv, greenLower, greenUpper)
    
    cv.imshow('onlycolor', mask)
    
    mask = cv.erode(mask, None, iterations=2)
    cv.imshow('erode', mask)
    mask = cv.dilate(mask, None, iterations=2)
    cv.imshow('dialte', mask)
   
    cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
    center = None
 
    if len(cnts) > 0:
        c = max(cnts, key=cv.contourArea)
        ((x, y), radius) = cv.minEnclosingCircle(c)
        M = cv.moments(c)
        center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]), time.time())
 
        if radius > 1:
            cv.circle(frame, (int(x), int(y)), int(radius), (255, 255, 255), 2)
            cv.circle(frame, (center[0], center[1]), 5, (0, 0, 255), -1)
 
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
 
    print("1|" + str(time.time()) + "|" + str(actualPointY) + "|" + str(actualPointX))
 
    # uncomment to see vid through test
    cv.imshow('frame', frame)

    if cv.waitKey(20) & 0xFF == ord('q'):

        break

# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
