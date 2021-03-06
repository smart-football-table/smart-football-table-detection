import cv2
import time

if __name__ == '__main__' :

    # Start default camera
    video = cv2.VideoCapture(0);

    video.set(cv2.CAP_PROP_FRAME_WIDTH,160);
    video.set(cv2.CAP_PROP_FRAME_HEIGHT,120);

    fps = video.get(cv2.CAP_PROP_FPS)
    print "Frames per second using video.get(cv2.CAP_PROP_FPS) : {0}".format(fps)


    # Number of frames to capture
    num_frames = 120;


    print "Capturing {0} frames".format(num_frames)

    # Start time
    start = time.time()




    # Grab a few frames
    for i in xrange(0, num_frames) :
        ret, frame = video.read()


    # End time
    end = time.time()

    # Time elapsed
    seconds = end - start
    print "Time taken : {0} seconds".format(seconds)

    # Calculate frames per second
    fps  = num_frames / seconds;
    print "Estimated frames per second : {0}".format(fps);

    # Release video
    video.release()
