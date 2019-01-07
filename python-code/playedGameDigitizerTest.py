import unittest
from playedGameDigitizer import *


class MyOpenCVTest(unittest.TestCase):
    
    def testwriteOnePointInFile(self):
        
        pts = deque(maxlen=20000)

        pts.appendleft(0, 0, 100)
        
        writePositionsWithTimeInFile(pts)
        
        file = open("../java-code/ballMovementInCoordinates.txt")
        
        self.assertEqual(file.readline(), "0|0|100")


if __name__ == '__main__':
    unittest.main()
