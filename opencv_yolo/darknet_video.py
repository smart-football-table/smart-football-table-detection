import os
from collections import deque

import cv2 as cv

import customDarknet as darknet
from utils import arguments_handler, arguments_parser, detection_position_handler, config, draw_handler


def preprocess_frame(frame, width, height):
    frame_rgb = cv.cvtColor(frame, cv.COLOR_BGR2RGB)

    # cut out values for the fisheye lense, dirty hack during COM
    x_start = 9
    x_end = 440
    y_start = 0
    y_end = width

    frame_rgb = frame_rgb[x_start:x_end, y_start:y_end]

    frame_rgb = cv.resize(frame_rgb, (width, height))

    return cv.resize(frame_rgb,
                     (darknet.network_width(netMain),
                      darknet.network_height(netMain)),
                     interpolation=cv.INTER_LINEAR)


def detect_ball_with_darknet_yolo_and_draw_labeling(frame, darknet_image):
    darknet.copy_image_from_bytes(darknet_image, frame.tobytes())

    detections = darknet.detect_image(netMain, metaMain, darknet_image, thresh=0.05)

    if not (len(detections) is 0):
        idOfDetection = getIDHighestDetection(detections)
        position = (int(detections[idOfDetection][2][0]), int(detections[idOfDetection][2][1]))
        draw_handler.draw_labeling_for_yolo_detection(detections[idOfDetection], frame)
        points_to_draw_trace_with.appendleft(position)
    else:
        position = config.DEFAULT_POSITION

    return position


def getIDHighestDetection(detections):
    idOfMaxProbability = 0
    maxProbability = 0

    for index, detection in enumerate(detections):

        probability = detection[1]
        if (probability > maxProbability):
            maxProbability = probability
            idOfMaxProbability = index

    return idOfMaxProbability


netMain = None
metaMain = None
altNames = None

args = arguments_parser.parse_arguments()
color_lower_treshold, color_upper_treshold, path_to_file, trace_length, client = arguments_handler.define_values_from_arguments(
    args)

if args.recordmode:
    if args.recordpath != 'empty':
        fileName = args.record
        fourcc = cv.cv.FOURCC(*'XVID')
        out = cv.VideoWriter((str(fileName) + '.avi'), fourcc, 20.0, (800, 525))

points_to_draw_trace_with = deque(maxlen=trace_length)


def YOLO():
    global metaMain, netMain, altNames
    configPath = 'obj.cfg'
    weightPath = 'obj.weights'
    metaPath = 'obj.data'
    if not os.path.exists(configPath):
        raise ValueError("Invalid config path `" +
                         os.path.abspath(configPath) + "`")
    if not os.path.exists(weightPath):
        raise ValueError("Invalid weight path `" +
                         os.path.abspath(weightPath) + "`")
    if not os.path.exists(metaPath):
        raise ValueError("Invalid data file path `" +
                         os.path.abspath(metaPath) + "`")
    if netMain is None:
        netMain = darknet.load_net(configPath.encode(
            "ascii"), weightPath.encode("ascii"), 0, 1)  # batch size = 1
    if metaMain is None:
        metaMain = darknet.load_meta(metaPath.encode("ascii"))
    if altNames is None:
        try:
            with open(metaPath) as metaFH:
                metaContents = metaFH.read()
                import re
                match = re.search("names *= *(.*)$", metaContents,
                                  re.IGNORECASE | re.MULTILINE)
                if match:
                    result = match.group(1)
                else:
                    result = None
                try:
                    if os.path.exists(result):
                        with open(result) as namesFH:
                            namesList = namesFH.read().strip().split("\n")
                            altNames = [x.strip() for x in namesList]
                except TypeError:
                    pass
        except Exception:
            pass
    cap = cv.VideoCapture(path_to_file)

    width = int(cap.get(3))  # float
    height = int(cap.get(4))  # float

    # Create an image we reuse for each detect
    darknet_image = darknet.make_image(darknet.network_width(netMain),
                                       darknet.network_height(netMain), 3)

    while True:
        ret, frame = cap.read()

        frame = preprocess_frame(frame, width, height)

        position = detect_ball_with_darknet_yolo_and_draw_labeling(frame, darknet_image)

        draw_handler.draw_trace(frame, points_to_draw_trace_with)

        detection_position_handler.define_and_publish_detection_position(frame.shape, position, client)

        # TODO evtl. ist hier was kaputt gegangen, dies noch prüfen
        frame = cv.cvtColor(frame, cv.COLOR_BGR2RGB)
        frame = cv.resize(frame, (800, 525))

        if args.record is not 'empty':
            out.write(frame)

        if args.showvideo:
            cv.imshow('Demo', frame)

        cv.waitKey(3)

    cap.release()
    if args.record is not 'empty':
        out.release()


if __name__ == "__main__":
    YOLO()
