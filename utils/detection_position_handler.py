import time

from utils import config


def define_and_publish_detection_position(frame_shape, position, client):
    if position == config.DEFAULT_POSITION:
        rel_point_x = position[0]
        rel_point_y = position[1]
    else:
        rel_point_x = float(position[0]) / frame_shape[1]
        rel_point_y = float(position[1]) / frame_shape[0]  # 0=rows

    timepoint = int(time.time() * 1000)

    client.publish("ball/position/rel", str(timepoint) + "," + str(rel_point_x) + "," + str(rel_point_y))
