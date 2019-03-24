from collections import deque
import cv2 as cv
import numpy as np
import imutils

frameSize = 800

pts = deque(maxlen=20000)

cap = cv.VideoCapture(0)

cap.set(28, 0)

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()

    frame = imutils.resize(frame, width=frameSize)

    img = cv.medianBlur(frame,5)
    cimg = cv.cvtColor(img,cv.COLOR_GRAY2BGR)

    circles = cv.HoughCircles(img,cv.HOUGH_GRADIENT,1,20,
    param1=50,param2=30,minRadius=0,maxRadius=0)

    circles = np.uint16(np.around(circles))
    for i in circles[0,:]:
        # draw the outer circle
        cv.circle(cimg,(i[0],i[1]),i[2],(0,255,0),2)
        # draw the center of the circle
        cv.circle(cimg,(i[0],i[1]),2,(0,0,255),3)


    # uncomment to see vid through test
    cv.imshow('frame', cimg)

    if cv.waitKey(1) & 0xFF == ord('q'):

        break

# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
