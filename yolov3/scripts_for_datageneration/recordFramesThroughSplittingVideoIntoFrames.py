import numpy as np
import cv2

cap = cv2.VideoCapture('../../../Schreibtisch/testvideos/schief.avi')


onlyOnePerTwentyFrames = 0
i = 0

n_rows = 5
n_images_per_row = 5


while(cap.isOpened()):

    ret, frame = cap.read()
    
    if(onlyOnePerTwentyFrames == 20):
        
        onlyOnePerTwentyFrames = 0
        i= i+1
    
        height, width, ch = frame.shape

        roi_height = (int) (height / n_rows)
        roi_width = (int) (width / n_images_per_row)

        for x in range(0, n_rows):
            for y in range(0,n_images_per_row):
                tmp_image=frame[x*roi_height:(x+1)*roi_height, y*roi_width:(y+1)*roi_width]
    
                out = cv2.imwrite("../../../Schreibtisch/testvideos/frames/vid-1-f%s-x%s-y%s.jpg" % (i,x,y), tmp_image)


    onlyOnePerTwentyFrames += 1

    
    if cv2.waitKey(0) & 0xFF == ord('q'):
        print("done")
        break

cap.release()
cv2.destroyAllWindows()
