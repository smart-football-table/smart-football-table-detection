import unittest
from testedPlayedGameDigitizer import *

class MyOpenCVTest(unittest.TestCase):

    def test_hello(self):
        self.assertEqual(hello_world(), "hello world")


if __name__ == '__main__':
    unittest.main()
