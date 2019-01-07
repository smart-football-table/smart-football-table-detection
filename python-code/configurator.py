
ballSize = 3.4
soccerTableLength = 120
soccerTableWidth = 69
frameResolution = 800

pixelPerCentimeter = 0
gameFieldWidth = 0
gameFieldLength = 0


def setBallSize(newBallSize):
    ballSize = newBallSize
def setSoccerTableLength(newSoccerTableLength):
    soccerTableLength = newSoccerTableLength
def setSoccerTableWidth(newSoccerTableWidth):
    soccerTableWidth = newSoccerTableWidth
def setFrameResolution(newFrameResolution):
    frameResolution = newFrameResolution

def getPixelPerCentimeter():
    return pixelPerCentimeter
def getGameFieldWidth():
    return gameFieldWidth
def getGameFieldLength():
    return gameFieldLength


def getBallSize():
    return ballSize;
def getSoccerTableLength():
    return soccerTableLength;
def getSoccerTableWidth():
    return soccerTableWidth;
def getFrameResolution():
    return frameResolution;

def calculatePixelPerCentimeter(ballSizeInPixel):
    pixelPerCentimeter = round(ballSize/ballSizeInPixel, 2);

def calculateFieldLength(ballSizeInPixel, ballPosition1_X, ballPosition2_X):
    gameFieldLength = round(getPixelPerCentimeter(ballSizeInPixel)*(ballPosition2_X-ballPosition1_X) ,2)

def calculateFieldWidth(ballSizeInPixel, ballPosition1_Y, ballPosition2_Y):
    gameFieldWidth = round(getPixelPerCentimeter(ballSizeInPixel)*(ballPosition2_Y-ballPosition1_Y) ,2)
