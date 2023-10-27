from collections import deque
import cv2 as cv
import imutils
import time
import numpy as np
import paho.mqtt.client as mqtt

from utils import arguments_parser, arguments_handler


def draw_detection(int, x, cv, frame, center, y, radius):
    cv.circle(frame, (int(x), int(y)), int(radius), (255, 255, 255), 2)
    cv.circle(frame, (center[0], center[1]), 5, (0, 0, 255), -1)


def prepare_frame(colorLower, colorUpper, frameSize, cv, frame):
    frame = imutils.resize(frame, width=frameSize)
    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)
    mask = cv.inRange(hsv, colorLower, colorUpper)
    mask = cv.erode(mask, None, iterations=2)
    mask = cv.dilate(mask, None, iterations=2)
    return mask, frame


def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))


xrange = range  # to run xrange in python 3

# construct the argument parse and parse the arguments
args = arguments_parser.parse_arguments()

color_lower_treshold, color_upper_treshold, path_to_file, trace_length, mqttport = arguments_handler.define_values_from_arguments(
    args)

if args.recordmode:
    if args.recordpath != 'empty':
        fileName = args.record
        fourcc = cv.cv.FOURCC(*'XVID')
        out = cv.VideoWriter((str(fileName) + '.avi'), fourcc, 20.0, (800, 600))

# define framevars
frameSize = 800

pts = deque(maxlen=trace_length)

cap = cv.VideoCapture(path_to_file)

cap.set(28, 0)

# start mqttclient
client = mqtt.Client()
client.on_connect = on_connect

client.connect("localhost", mqttport, 60)

client.loop_start()

while (True):
    # Capture frame-by-frame
    ret, frame = cap.read()

    mask, frame = prepare_frame(color_lower_treshold, color_upper_treshold, frameSize, cv, frame)

    position = (-1, -1)

    cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]
    timepoint = int(time.time() * 1000)

    if len(cnts) > 0:
        c = max(cnts, key=cv.contourArea)
        ((x, y), radius) = cv.minEnclosingCircle(c)
        M = cv.moments(c)
        position = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))

        if radius > 1:
            draw_detection(int, x, cv, frame, position, y, radius)

    if not (position[0] == -1):
        pts.appendleft(position)

    # loop over the set of tracked points
    for i in xrange(1, len(pts)):
        # if either of the tracked points are None, ignore
        # them
        if pts[i - 1] is None or pts[i] is None:
            continue

        # otherwise, compute the thickness of the line and
        # draw the connecting lines
        thickness = int(np.sqrt(200 / float(i + 1)) * 2)
        cv.line(frame, pts[i - 1], pts[i], (0, 0, 255), thickness)

    if (position[0] == -1):
        relPointX = position[0]
        relPointY = position[1]
    else:
        relPointX = float(position[0]) / frame.shape[1]
        relPointY = float(position[1]) / frame.shape[0]  # 0=rows

    client.publish("ball/position/rel", str(timepoint) + "," + str(relPointX) + "," + str(relPointY))

    if args.recordmode and args.recordpath != 'empty':
        out.write(frame)

    cv.imshow('frame', frame)

    if cv.waitKey(20) & 0xFF == ord('q'):
        break

# When everything done, release the capture
cap.release()
out.release()
cv.destroyAllWindows()
