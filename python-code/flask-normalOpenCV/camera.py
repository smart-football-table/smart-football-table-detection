import cv2 as cv


class VideoCamera(object):
    
    
    def __init__(self):
        self.frame = cv.VideoCapture(0)
        self.frame.set(28, 0)
    
    def __del__(self):
        self.frame.release()
    
    def get_frame(self):
        success, image = self.frame.read()
        # We are using Motion JPEG, but OpenCV defaults to capture raw images,
        # so we must encode it into JPEG in order to correctly display the
        # video stream.
        ret, jpeg = cv.imencode('.jpg', image)
        return jpeg.tobytes()