from utils import mqtt_handler


def define_values_from_arguments(args):
    color_lower_treshold, color_upper_treshold = get_color_treshold(args)
    path_to_file = get_source_of_video(args)
    trace_length = get_trace_length(args)
    client = get_mqttclient(args)
    return color_lower_treshold, color_upper_treshold, path_to_file, trace_length, client


def get_color_treshold(args):
    x = args.color.split(",")
    hsv_min_hue = int(x[0])
    hsv_min_saturation = int(x[1])
    hsv_min_value = int(x[2])
    hsv_max_hue = int(x[3])
    hsv_max_saturation = int(x[4])
    hsv_max_value = int(x[5])
    color_lower_treshold = (hsv_min_hue, hsv_min_saturation, hsv_min_value)
    color_upper_treshold = (hsv_max_hue, hsv_max_saturation, hsv_max_value)
    return color_lower_treshold, color_upper_treshold


def get_source_of_video(args):
    if args.video != 'empty':
        return args.video
    else:
        return args.camindex


def get_trace_length(args):
    if args.buffer != 'empty':
        return args.buffer


def get_mqttclient(args):
    if args.mqttport != 'empty':
        client = mqtt_handler.start_mqttclient(args.mqtthost, args.mqttport)
        return client
