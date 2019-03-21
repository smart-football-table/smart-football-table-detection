from collections import deque
import cv2 as cv
import argparse
import imutils
import time
import os

parser = argparse.ArgumentParser()
parser.add_argument("a", nargs='?', default="empty")
args = parser.parse_args()

if args.a == 'empty':
    greenLower = (20, 100,100)
    greenUpper = (30,255, 255)
else:
    x = args.a.split(",")
    hsvminh = int(x[0])
    hsvmins = int(x[1])
    hsvminv = int(x[2])
    hsvmaxh = int(x[3])
    hsvmaxs = int(x[4])
    hsvmaxv = int(x[5])
    greenLower = (hsvminh,hsvmins,hsvminv)
    greenUpper = (hsvmaxh,hsvmaxs,hsvmaxv)


frameSize = 800

pts = deque(maxlen=20000)

pts.appendleft((0, 0, time.time()))
pts.appendleft((0, 0, time.time()))

pts2 = deque(maxlen=20000)

pts2.appendleft((0, 0, time.time()))
pts2.appendleft((0, 0, time.time()))

cap = cv.VideoCapture(0)
cap2 = cv.VideoCapture(1)

cap.set(28, 0)

cv.namedWindow("frame")
cv.moveWindow("frame", 20,20);
        
cv.namedWindow("frame2")
cv.moveWindow("frame2", 820,20);

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()
    ret2, frame2 = cap2.read()

    frame = imutils.resize(frame, width=frameSize)
    frame2 = imutils.resize(frame2, width=frameSize)

    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)

    mask = cv.inRange(hsv, greenLower, greenUpper)
    mask = cv.erode(mask, None, iterations=2)
    mask = cv.dilate(mask, None, iterations=2)

    hsv2 = cv.cvtColor(frame2, cv.COLOR_BGR2HSV)

    mask2 = cv.inRange(hsv2, greenLower, greenUpper)
    mask2 = cv.erode(mask2, None, iterations=2)
    mask2 = cv.dilate(mask2, None, iterations=2)

    cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
    center = None

    cnts2 = cv.findContours(mask2.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
    center2 = None

    if len(cnts) > 0:
        c = max(cnts, key=cv.contourArea)
        ((x, y), radius) = cv.minEnclosingCircle(c)
        M = cv.moments(c)
        center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]), time.time())

        if radius > 1:
            cv.circle(frame, (int(x), int(y)), int(radius), (255, 255, 255), 2)
            cv.circle(frame, (center[0], center[1]), 5, (0, 0, 255), -1)

    if len(cnts2) > 0:
        c2 = max(cnts2, key=cv.contourArea)
        ((x2, y2), radius2) = cv.minEnclosingCircle(c2)
        M2 = cv.moments(c2)
        center2 = (int(M2["m10"] / M2["m00"]), int(M2["m01"] / M2["m00"]), time.time())

        if radius2 > 1:
            cv.circle(frame2, (int(x2), int(y2)), int(radius2), (255, 255, 255), 2)
            cv.circle(frame2, (center2[0], center2[1]), 5, (0, 0, 255), -1)

    if(center == None):
        center = (-1, -1, time.time())

    if(center2 == None):
        center2 = (-1, -1, time.time())

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
    
    pts2.appendleft(center2)

    if pts2[0] is None or pts2[1] is None:
        actualPointX, actualPointY = 0, 0
        previousPointX, previousPointY = 0, 0
    else:
        actualPointX = pts2[1][0]
        actualPointY = pts2[1][1]
        previousPointX = pts2[0][0]
        previousPointY = pts2[0][1]

    print("2|" + str(time.time()) + "|" + str(actualPointY) + "|" + str(actualPointX))

    # uncomment to see vid through test
    cv.imshow('frame', frame)
    cv.imshow('frame2', frame2)

    if cv.waitKey(1) & 0xFF == ord('q'):

        break

# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
