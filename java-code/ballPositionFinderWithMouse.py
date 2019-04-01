import numpy as np
import cv2

def click_and_crop(event, x, y, flags, param):
    global x_start, y_start, cropping
    
    if event == cv2.EVENT_LBUTTONDOWN:
        x_start, y_start = x, y
        cropping = True
        
    elif event == cv2.EVENT_MOUSEMOVE:
        x_start, y_start = x, y
        
    elif event == cv2.EVENT_LBUTTONUP:
        cropping = False
        
    
cv2.namedWindow("cam1")
cv2.setMouseCallback("cam1", click_and_crop)

x_start=0;
y_start=0;
cropping=False;

listing = os.listdir('../../../Schreibtisch/testvideos/testfor')    
for file in listing:
    cap = cv2.VideoCapture('../../../Schreibtisch/testvideos/testfor')

    ret, frame = cap.read()

    treshold =  100
    
    x_end = x_start+treshold
    y_end = y_start+treshold
    
    upper_left = (x_start, y_start)
    bottom_right = (x_end, y_end)
    
    tmp_image=frame[upper_left[1]+2 : bottom_right[1]-2, upper_left[0]+2 : bottom_right[0]-2]
    cv2.rectangle(frame, upper_left, bottom_right, (0, 255, 0), 2)
    
    cv2.imshow("cam1", frame)
    
    if(cropping == True):    
        out = cv2.imwrite("../../../Schreibtisch/testvideos/frames-high/vid-4-%s.jpg" % i, tmp_image)
        i= i+1
    
    if cv2.waitKey(1) & 0xFF == ord('q'):
        print("done")
        break

cap.release()
cv2.destroyAllWindows()
