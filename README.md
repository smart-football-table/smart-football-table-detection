# Smart Football Table - Detection

This repository contains the detection part of the Smart Football Table project. After getting the position of the ball, calculations are done. The detection is done with a python-script which is called from a java-programm, which handles the position afterwards.

### Summary

1) Detect the ball
  * with shape search (OpenCV)
  * with machine learning (OpenCV+YOLO)
2) Use ball positions to calculate different things
  * goal detection
  * ball velocity
  * trace
  
### Detection in detail

We are using OpenCV to detect the ball on gamefield. At the moment, there exist two solutions:

* single OpenCV ball detection, with image preprocessing and shape search:
  * get input image (file or webcam)
  * convert to hsv color range
  * mask image to color range
  * erode and dilate the picture
  * find contours
  
* YOLO ball detection:
  * get input image (file or webcam)
  * find objects based on training data, which is he ball in this case
  * for more, see README in yolo3 folder
  
### Arguments for python scripts

| Argument | Description                                   | Sample Input           | default |
| -- | --------------------------------------------------- | ---------------------- | ----- |
| -v | path to an (optional) video file, overwrites camera | "-v path/to/video.avi" | empty |
| -b | length of lightning trace                           | 64                     | 200 |
| -i | index of camera                                     | 0                      | 0 |
| -c | color values (hsvmin,hsvmax) for object you want to detect (unneccessary for yolo) | 20,100,100,30,255,255 | 0,0,0,0,0,0 |
| -r | recording output into given file name               | "fileName"             | empty |
