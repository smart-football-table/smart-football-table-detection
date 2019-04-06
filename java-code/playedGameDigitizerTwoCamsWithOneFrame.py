from collections import deque
import cv2 as cv
import argparse
import imutils
import time
import numpy as np
import os

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

cap = cv.VideoCapture(0)
cap2 = cv.VideoCapture("../../../Schreibtisch/testvideos/bad_light.avi")

cap.set(28, 0)

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()
    ret2, frame2 = cap2.read()

    frame = imutils.resize(frame, width=frameSize)
    frame2 = imutils.resize(frame2, width=frameSize)


    x1_start = 200
    x1_end = 300
    y1_start = 10
    y1_end = 300
    
    x2_start = 10
    x2_end = 300
    y2_start = 10
    y2_end = 300
    
    frame = frame[x1_start:x1_end, y1_start:y1_end]
    frame2 = frame2[x2_start:x2_end, y2_start:y2_end]

    frame = cv.resize(frame, (600, 800))
    frame2 = cv.resize(frame2, (600, 800))
    
    frame = np.concatenate((frame, frame2), axis=1)

    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)

    mask = cv.inRange(hsv, greenLower, greenUpper)
    mask = cv.erode(mask, None, iterations=2)
    mask = cv.dilate(mask, None, iterations=2)

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

    print("1|" + str(time.time()) + "|" + str(actualPointX) + "|" + str(actualPointY))
    print("2|" + str(time.time()) + "|" + str(actualPointY) + "|" + str(actualPointX))

    cv.imshow('out', frame)

    if cv.waitKey(1) & 0xFF == ord('q'):

        break

# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
