import unittest
#import playedGameDigitizer
import sys

class test_playedGameDigitizer(unittest.TestCase):

    def test_import(self):
        """ Test that the cv2 module can be imported. """
        import cv2

    def test_video_capture(self):

        import cv2
        cap = cv2.VideoCapture("samplevideos/testVid_ballUpperLeft")
        self.assertTrue(cap.isOpened())

if __name__ == '__main__':
    unittest.main()
