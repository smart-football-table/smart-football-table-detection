import unittest
import configurator

class test_configurator(unittest.TestCase):

    def test_readCorrectValues(self):
        self.assertEqual(configurator.getBallSize(), 3.4)
        self.assertEqual(configurator.getSoccerTableLength(), 120)
        self.assertEqual(configurator.getSoccerTableWidth(), 69)
        self.assertEqual(configurator.getFrameResolution(), 800)

    def test_calculateCorrectPixelPerCentimeter(self):

        configurator.setBallSize(3.4)
        configurator.setSoccerTableLength(120)
        configurator.setSoccerTableWidth(69)
        configurator.setFrameResolution(800)

        ballSizeInPixel = 30

        configurator.calculatePixelPerCentimeter(ballSizeInPixel)

        self.assertEqual(configurator.getPixelPerCentimeter(), 0.11)

    def test_calculateCorrectGameFieldSize(self):

        configurator.setBallSize(3.4)
        configurator.setSoccerTableLength(120)
        configurator.setSoccerTableWidth(69)
        configurator.setFrameResolution(800)

        ballSizeInPixel = 10
        ballPosition1_X = 50
        ballPosition1_Y = 70
        ballPosition2_X = 650
        ballPosition2_Y = 650

        configurator.calculateFieldLength(ballSizeInPixel, ballPosition1_X,  ballPosition2_X)
        configurator.calculateFieldWidth(ballSizeInPixel, ballPosition1_Y,  ballPosition2_Y)

        self.assertEqual(configurator.getFieldLength(), 204)
        self.assertEqual(configurator.getFieldWidth(), 197.2)


# isnt able to configure color yet

if __name__ == '__main__':
    unittest.main()
