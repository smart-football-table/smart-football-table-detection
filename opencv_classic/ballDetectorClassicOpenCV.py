from collections import deque
import cv2 as cv
import imutils
import time

from utils import arguments_parser, arguments_handler, config, draw_handler


def preprocess_image_with_color_treshold(colorLower, colorUpper, cv, frame):
    frame = imutils.resize(frame, width=config.FRAMESIZE_IN_PIXEL)
    hsv = cv.cvtColor(frame, cv.COLOR_BGR2HSV)
    mask = cv.inRange(hsv, colorLower, colorUpper)
    mask = cv.erode(mask, None, iterations=2)
    mask = cv.dilate(mask, None, iterations=2)
    return mask, frame


def define_and_publish_detection_position(position):
    if position[0] == -1:
        relPointX = position[0]
        relPointY = position[1]
    else:
        relPointX = float(position[0]) / frame.shape[1]
        relPointY = float(position[1]) / frame.shape[0]  # 0=rows

    timepoint = int(time.time() * 1000)

    client.publish("ball/position/rel", str(timepoint) + "," + str(relPointX) + "," + str(relPointY))


def detect_ball_draw_circle_and_return_position():
    cnts = cv.findContours(mask.copy(), cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)[-2]

    if len(cnts) > 0:
        c = max(cnts, key=cv.contourArea)
        ((x, y), radius) = cv.minEnclosingCircle(c)
        M = cv.moments(c)

        if radius > 1:
            draw_handler.draw_detection_circle(frame, x, y, radius)

        position = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
    else:
        position = config.DEFAULT_POSITION

    return position


args = arguments_parser.parse_arguments()
color_lower_treshold, color_upper_treshold, path_to_file, trace_length, client = arguments_handler.define_values_from_arguments(
    args)

if args.recordmode:
    if args.recordpath != 'empty':
        fileName = args.record
        fourcc = cv.cv.FOURCC(*'XVID')
        out = cv.VideoWriter((str(fileName) + '.avi'), fourcc, 20.0, (800, 600))

points_to_draw_trace_with = deque(maxlen=trace_length)

cap = cv.VideoCapture(path_to_file)

cap.set(28, 0)

# start the frame loop, capture frame-by-frame
while (True):
    ret, frame = cap.read()

    mask, frame = preprocess_image_with_color_treshold(color_lower_treshold, color_upper_treshold, cv, frame)

    position = detect_ball_draw_circle_and_return_position()

    if not (position == config.DEFAULT_POSITION):
        points_to_draw_trace_with.appendleft(position)

    draw_handler.draw_trace(frame, points_to_draw_trace_with)
    draw_handler.draw_detection_center(frame, position)

    define_and_publish_detection_position(position)

    if args.recordmode and args.recordpath != 'empty':
        out.write(frame)

    cv.imshow('frame', frame)

    if cv.waitKey(20) & 0xFF == ord('q'):
        break

# When everything done, release the capture
cap.release()
if args.recordmode and args.recordpath != 'empty':
    out.release()
cv.destroyAllWindows()
