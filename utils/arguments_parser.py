import argparse
from utils import config

def parse_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("-v", "--video", default=config.VIDEO_PATH, help="path to the (optional) video file")
    parser.add_argument("-b", "--buffer", type=int, default=config.MAX_TRACE_LENGTH, help="max buffer size for lightning track")
    parser.add_argument("-i", "--camindex", default=config.CAM_INDEX, type=int, help="index of camera")
    parser.add_argument("-c", "--color", default=config.COLOR, help="color values comma seperated in hsv color model")
    parser.add_argument("-r", "--recordmode", default=config.RECORD_MODE, help="switch on recoding into file")
    parser.add_argument("-p", "--recordpath", default=config.RECORD_FILE_NAME, help="recording with following file name, recordmode must be true")
    parser.add_argument("-m", "--mqttport", default=config.MQTTPORT, help="sets the mqtt broker port")
    return parser.parse_args()