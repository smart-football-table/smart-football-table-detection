import numpy as np

from utils import config
import cv2 as cv


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
