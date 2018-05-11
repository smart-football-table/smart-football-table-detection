from collections import deque
import numpy as np
import cv2 as cv
import argparse
import imutils

# construct the argument parse and parse the arguments
ap = argparse.ArgumentParser()
ap.add_argument("-v", "--video",
	help="path to the (optional) video file")
ap.add_argument("-b", "--buffer", type=int, default=64,
	help="max buffer size")
args = vars(ap.parse_args())

# define the lower and upper boundaries of the "green"
# ball in the HSV color space, then initialize the
# list of tracked points
greenLower = (29, 86, 6)
greenUpper = (64, 255, 255)
pts = deque(maxlen=args["buffer"])

pts.appendleft((0,0))
pts.appendleft((0,0))


cap = cv.VideoCapture(0)

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()

	# resize the frame and convert it to the HSV
	# color space
    frame = imutils.resize(frame, width=600)

    # Our operations on the frame come here
    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)

    # define range of blue color in HSV,  then perform
    # a series of dilations and erosions to remove any small
    # blobs left in the mask
    #lower_blue = np.array([110,78,50])
    #upper_blue = np.array([130,255,255])
    mask = cv.inRange(hsv, greenLower, greenUpper)
    mask = cv.erode(mask, None, iterations=2)
    mask = cv.dilate(mask, None, iterations=2)

	# find contours in the mask and initialize the current
	# (x, y) center of the ball
    cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
    center = None

    # only proceed if at least one contour was found

    if len(cnts) > 0:
		# find the largest contour in the mask, then use
		# it to compute the minimum enclosing circle and
		# centroid
        c = max(cnts, key=cv.contourArea)
        ((x, y), radius) = cv.minEnclosingCircle(c)
        M = cv.moments(c)
        center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))

		# only proceed if the radius meets a minimum size
        if radius > 10:
			# draw the circle and centroid on the frame,
			# then update the list of tracked points
            cv.circle(frame, (int(x), int(y)), int(radius), (255,255, 255), 2)
            cv.circle(frame, center, 5, (0, 0, 255), -1)


    # update the points queue
    pts.appendleft(center)
    
    treshold = 20;
    
    if pts[0] is None or pts[1] is None or (pts[0][0]-pts[1][0] < 7 and pts[0][0]-pts[1][0] > -7 and pts[0][1]-pts[1][1] < 7 and pts[0][1]-pts[1][1] > -7):
    	print("No movement")
    else:
    	if pts[1][0] < pts[0][0]:
    		if pts[1][1] < pts[0][1]:
    			print("North")
    		else:
    			print("South")
    	else:
    		if pts[1][1] < pts[0][1]:
    			print("West")
    		else:
    			print("East")

	# loop over the set of tracked points
    for i in range(1, len(pts)):

		# if either of the tracked points are None, ignore
		# them
        if pts[i - 1] is None or pts[i] is None:
            continue

		# otherwise, compute the thickness of the line and
		# draw the connecting lines
        thickness = int(np.sqrt(args["buffer"] / float(i + 1)) * 2.5)
        cv.line(frame, pts[i - 1], pts[i], (0, 0, 255), thickness)
        
    #res = cv.bitwise_and(frame,frame, mask= mask)
    # Display the resulting frame
    cv.imshow('frame',frame)

    if cv.waitKey(1) & 0xFF == ord('q'):
        break
# When everything done, release the capture
cap.release()
cv.destroyAllWindows()
