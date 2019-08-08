import cv2 as cv


class VideoCamera(object):


    def __init__(self, pathToFile):
        self.frame = cv.VideoCapture(pathToFile)
        self.width = int(self.frame.get(3))  # float
        self.height = int(self.frame.get(4)) # float

    def __del__(self):
        self.frame.release()
