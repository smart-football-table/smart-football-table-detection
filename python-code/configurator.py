
ballSize = 3.4
soccerTableLength = 120
soccerTableWidth = 69
frameResolution = 800


def setBallSize(newBallSize):
    ballSize = newBallSize
def setSoccerTableLength(newSoccerTableLength):
    soccerTableLength = newSoccerTableLength
def setSoccerTableWidth(newSoccerTableWidth):
    soccerTableWidth = newSoccerTableWidth
def setFrameResolution(newFrameResolution):
    frameResolution = newFrameResolution

def getBallSize():
    return ballSize;
def getSoccerTableLength():
    return soccerTableLength;
def getSoccerTableWidth():
    return soccerTableWidth;
def getFrameResolution():
    return frameResolution;

def getPixelPerCentimeter(ballSizeInPixel):
    return round(ballSize/ballSizeInPixel, 2);

def getFieldLength(ballSizeInPixel, ballPosition1_X, ballPosition2_X):
    return round(getPixelPerCentimeter(ballSizeInPixel)*(ballPosition2_X-ballPosition1_X) ,2)

def getFieldWidth(ballSizeInPixel, ballPosition1_Y, ballPosition2_Y):
    return round(getPixelPerCentimeter(ballSizeInPixel)*(ballPosition2_Y-ballPosition1_Y) ,2)
