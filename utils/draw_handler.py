import numpy as np

from utils import config
import cv2 as cv


def draw_labeling_for_yolo_detection(detection, img):
    x, y, w, h = detection[2][0], \
        detection[2][1], \
        detection[2][2], \
        detection[2][3]
    xmin, ymin, xmax, ymax = convertDetectionValuesBackForDrawing(float(x), float(y), float(w), float(h))
    pt1 = (xmin, ymin)
    pt2 = (xmax, ymax)

    cv.rectangle(img, pt1, pt2, (0, 255, 0), 1)

    cv.putText(img, detection[0].decode() + " [" + str(round(detection[1] * 100, 2)) + "]", (pt1[0], pt1[1] - 5),
               cv.FONT_HERSHEY_SIMPLEX, 0.5, [0, 255, 0], 2)


def draw_detection_circle(frame, x, y, radius):
    cv.circle(frame, (int(x), int(y)), int(radius), config.WHITE, 2)


def draw_detection_center(frame, position):
    cv.circle(frame, (position[0], position[1]), 2, config.WHITE, -1)


def draw_trace(frame, points_to_draw_trace_with):
    xrange = range  # to run xrange in python 3
    for i in xrange(1, len(points_to_draw_trace_with)):
        # if either of the tracked points are None, ignore them
        if points_to_draw_trace_with[i - 1] is None or points_to_draw_trace_with[i] is None:
            continue

        # otherwise, compute the thickness of the line and
        thickness = int(np.sqrt(200 / float(i + 1)) * 2)
        # draw the connecting lines
        cv.line(frame, points_to_draw_trace_with[i - 1], points_to_draw_trace_with[i], config.RED, thickness)

def convertDetectionValuesBackForDrawing(x, y, w, h):
    xmin = int(round(x - (w / 2)))
    xmax = int(round(x + (w / 2)))
    ymin = int(round(y - (h / 2)))
    ymax = int(round(y + (h / 2)))
    return xmin, ymin, xmax, ymax
