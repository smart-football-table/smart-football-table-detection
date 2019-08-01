import cv2 as cv


class VideoCamera(object):
    
    
    def __init__(self):
        self.frame = cv.VideoCapture(0)
        self.frame.set(28, 0)
    
    def __del__(self):
        self.frame.release()