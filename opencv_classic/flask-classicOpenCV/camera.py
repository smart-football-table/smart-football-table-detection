import cv2 as cv


class VideoCamera(object):
    
    
    def __init__(self, pathToFile):
        self.frame = cv.VideoCapture(pathToFile)
        self.frame.set(28, 0)
    
    def __del__(self):
        self.frame.release()