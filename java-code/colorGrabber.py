# import the necessary packages
import numpy as np
import imutils
import cv2

# initialize the list of reference points and boolean indicating
# whether cropping is being performed or not
x_start, y_start, x_end, y_end = 0, 0, 0, 0
cropping = False
getROI = False
refPt = []
lower = np.array([])
upper = np.array([])

mode = 0

camera = cv2.VideoCapture(0)
camera2 = cv2.VideoCapture(1)


def click_and_crop(event, x, y, flags, param):
    # grab references to the global variables
    global x_start, y_start, x_end, y_end, cropping, getROI

    # if the left mouse button was clicked, record the starting
    # (x, y) coordinates and indicate that cropping is being
    # performed
    if event == cv2.EVENT_LBUTTONDOWN:
        x_start, y_start, x_end, y_end = x, y, x, y
        cropping = True

    elif event == cv2.EVENT_MOUSEMOVE:
        if cropping == True:
            x_end, y_end = x, y

    # check to see if the left mouse button was released
    elif event == cv2.EVENT_LBUTTONUP:
        # record the ending (x, y) coordinates and indicate that
        # the cropping operation is finished
        x_end, y_end = x, y
        cropping = False
        getROI = True


cv2.namedWindow("cam1")
cv2.setMouseCallback("cam1", click_and_crop)

# keep looping
while True:

    if (mode == 0):
    
        if not getROI:
    
            while True:
                # grab the current frame
                (grabbed, frame) = camera.read()
    
                if not grabbed:
                    break
    
                if not cropping and not getROI:
                    cv2.imshow("cam1", frame)
    
                elif cropping and not getROI:
                    cv2.rectangle(frame, (x_start, y_start), (x_end, y_end), (0, 255, 0), 2)
                    cv2.imshow("cam1", frame)
    
                elif not cropping and getROI:
                    cv2.rectangle(frame, (x_start, y_start), (x_end, y_end), (0, 255, 0), 2)
                    cv2.imshow("cam1", frame)
                    break
    
                key = cv2.waitKey(45) & 0xFF
                # if the 'q' key is pressed, stop the loop
                if key == ord("q"):
                    noROI = True
                    break
    
            # if there are two reference points, then crop the region of interest
            # from teh image and display it
            refPt = [(x_start, y_start), (x_end, y_end)]
    
            roi = frame[refPt[0][1]:refPt[1][1], refPt[0][0]:refPt[1][0]]
            # cv2.imshow("ROI", roi)
    
            hsvRoi = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)
            # this is for nice output
            # print('min H = {}, min S = {}, min V = {}; max H = {}, max S = {}, max V = {}'.format(hsvRoi[:,:,0].min(), hsvRoi[:,:,1].min(), hsvRoi[:,:,2].min(), hsvRoi[:,:,0].max(), hsvRoi[:,:,1].max(), hsvRoi[:,:,2].max()))
            # print('{},{},{},{},{},{}'.format(hsvRoi[:, :, 0].min(), hsvRoi[:, :, 1].min(), hsvRoi[:, :, 2].min(), hsvRoi[:, :, 0].max(), hsvRoi[:, :, 1].max(), hsvRoi[:, :, 2].max()))
    
            # file = open("hsvminmax.txt","w")
    
            # file.write('{}|{}|{}|{}|{}|{}'.format(hsvRoi[:,:,0].min(), hsvRoi[:,:,1].min(), hsvRoi[:,:,2].min(), hsvRoi[:,:,0].max(), hsvRoi[:,:,1].max(), hsvRoi[:,:,2].max()))
            # file.close()
            lower = np.array([hsvRoi[:, :, 0].min(), hsvRoi[:, :, 1].min(), hsvRoi[:, :, 2].min()])
            upper = np.array([hsvRoi[:, :, 0].max(), hsvRoi[:, :, 1].max(), hsvRoi[:, :, 2].max()])
    
        # grab the current frame
        (grabbed, frame) = camera.read()
    
        if not grabbed:
            break
    
        # resize the frame, blur it, and convert it to the HSV
        # color space
        # frame = imutils.resize(frame, width=600)
    
        blurred = cv2.GaussianBlur(frame, (11, 11), 0)
        hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
    
        # construct a mask for the color from dictionary`1, then perform
        # a series of dilations and erosions to remove any small
        # blobs left in the mask
        kernel = np.ones((9, 9), np.uint8)
        mask = cv2.inRange(hsv, lower, upper)
        mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    
        # find contours in the mask and initialize the current
        # (x, y) center of the ball
        cnts = cv2.findContours(mask.copy(), cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE)[-2]
        center = None
    
        
    
        if len(cnts) > 0:
            c = max(cnts, key=cv2.contourArea)
            ((x, y), radius) = cv2.minEnclosingCircle(c)
            M = cv2.moments(c)
            center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
    
            if radius > 0.5:
                cv2.circle(frame, (int(x), int(y)), int(radius), (0, 255, 0), 2)
                
        cv2.imshow("cam1", frame)
    
    if (mode == 1):
        
        print('{},{},{},{},{},{}'.format(hsvRoi[:, :, 0].min(), hsvRoi[:, :, 1].min(), hsvRoi[:, :, 2].min(), hsvRoi[:, :, 0].max(), hsvRoi[:, :, 1].max(), hsvRoi[:, :, 2].max()))
        mode = mode + 1

    if (mode == 2) or (mode == 4) or (mode == 6) or (mode == 8):

        (grabbed, frame) = camera.read()
        (grabbed2, frame2) = camera2.read()
        
        blurred = cv2.GaussianBlur(frame, (11, 11), 0)
        hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
        
        blurred2 = cv2.GaussianBlur(frame2, (11, 11), 0)
        hsv2 = cv2.cvtColor(blurred2, cv2.COLOR_BGR2HSV)
    
        mask = cv2.inRange(hsv, lower, upper)
        mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
        mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
        
        mask2 = cv2.inRange(hsv2, lower, upper)
        mask2 = cv2.morphologyEx(mask2, cv2.MORPH_OPEN, kernel)
        mask2 = cv2.morphologyEx(mask2, cv2.MORPH_CLOSE, kernel)
        
        
        cnts = cv2.findContours(mask.copy(), cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE)[-2]
        center = None
        
        cnts2 = cv2.findContours(mask2.copy(), cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE)[-2]
        center2 = None
    
        if len(cnts) > 0:
            c = max(cnts, key=cv2.contourArea)
            ((x, y), radius) = cv2.minEnclosingCircle(c)
            M = cv2.moments(c)
            center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
    
            if radius > 0.5:
                cv2.circle(frame, (int(x), int(y)), int(radius), (0, 255, 0), 2)
                
        if len(cnts2) > 0:
            c2 = max(cnts2, key=cv2.contourArea)
            ((x2, y2), radius2) = cv2.minEnclosingCircle(c2)
            M2 = cv2.moments(c2)
            center2 = (int(M2["m10"] / M2["m00"]), int(M2["m01"] / M2["m00"]))

            if radius2 > 0.5:
               cv2.circle(frame2, (int(x2), int(y2)), int(radius2), (0, 255, 0), 2)
        
        cv2.imshow("cam1", frame)
        cv2.imshow("cam2", frame2)
        
        
    if (mode == 3) or (mode == 5) or (mode == 7) or (mode == 9):
        
        if center == None:
            center = (-1,-1)
        if center2 == None:
            center2 = (-1,-1)
        print('{},{},{},{}'.format(center[0],center[1],center2[0],center2[1]))
        mode = mode + 1
        
    if (mode == 10):
        
        print(int(radius))
        break
        
    key = cv2.waitKey(45) & 0xFF
    # if the 'q' key is pressed, stop the loop
    if key == ord("q"):
        break
    elif key == ord("r"):
        getROI = False
    elif key == ord("n"):
        mode = mode + 1

# cleanup the camera and close any open windows
camera.release()
cv2.destroyAllWindows()
