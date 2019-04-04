# Stuff to know

[YOLO](https://pjreddie.com/darknet/yolo/)

## Usage

Contains a really short step-by-step list. Check the article section for some really good tutorials.

#### Installation

1) Install [CUDA](https://developer.nvidia.com/cuda-toolkit) (--> be sure to have matching versions for CUDA, drivers and your linux distribution)

2) Install [OpenCV](https://docs.opencv.org/trunk/d7/d9f/tutorial_linux_install.html) sources (not lib)

3) Clone [Darknet](https://github.com/pjreddie/darknet) (or this one [AlexeyAB-Darknet](https://github.com/AlexeyAB/darknet). You often read it is the better one. Everything below is only tested with the common solution, but most stuff should work the same. Imporant: their readme is great for tips and information)

#### Training

1) Have data! According to Yolo at least 2000 per class you want to detect.

2) Create for each image a txt-file containing classes on your image, their x and y pos and the size of them (yolo needs this information). Use tools for this (like [OpenLabeling](https://github.com/Cartucho/OpenLabeling))!

3) Create test.txt and train.txt (use a splitter or your labeling tool)

4) create obj.data (what parameters), obj.names (which classes) and .cfg (the neural network settings). Use for the neural network one of the countless examples in darknet/cfg-folder

5) Optional: download pre-trained weights

6) cd to darknet and run './darknet detector train path/to/obj.data path/to/.cfg path/to/weights'

7) wait (one class, GTX1060, roughly 10hours), then stop the training yourself

8) check the trained weights (with default the steps are 100,200,300,...,900,1000,10000,20000,...)

9) test your model on some test data (this doesn't need CUDA or OpenCV) './darknet detector test path/to/obj.data path/to/.cfg path/to/weights path/to/image.jpg'

10) test your model on some videos (this needs CUDA and OpenCV) './darknet detector demo path/to/obj.data path/to/.cfg path/to/weights path/to/video.avi'

11) play with your parameters


## Some comments to what we did

* we used tiny-yolov3 cfg base
* we filmed the soccer table and created data from it
* single-class: ball
* only yellow ball with green ground (expand later)
* base weight darknet53.conv.74
* one Nvidia GeForce GTX 980
* cfg params:
    * batch = 12 -> loading 12 images for one iteration
    * subdivisions = 1 -> split batch
    * classes = 1
* roughly 12 hours of training, 130,000 iterations

## Links & Articles

* [YOLO Paper](https://arxiv.org/pdf/1506.02640.pdf)
* [YOLO9000](https://arxiv.org/pdf/1612.08242.pdf) (also a good paper)
* [YOLO Explanation](https://medium.com/@jonathan_hui/real-time-object-detection-with-yolo-yolov2-28b1b93e2088)
* [CUDA&OpenCV-Docker](https://medium.com/techlogs/compiling-opencv-for-cuda-for-yolo-and-other-cnn-libraries-9ce427c00ff8)
* [AlexeyAB Version of Darknet](https://github.com/AlexeyAB/darknet) (read the readme!)
* [How to train with custom data](https://timebutt.github.io/static/how-to-train-yolov2-to-detect-custom-objects/) (short explanation, but hits the point)
* article about [faced](https://towardsdatascience.com/faced-cpu-real-time-face-detection-using-deep-learning-1488681c1602) (some values to speed)
* [CPU Running Time Reduction](http://guanghan.info/blog/en/my-works/yolo-cpu-running-time-reduction-basic-knowledge-and-strategies/)

## some Cuda stuff...

Your darknet "make"-command doesn't work becaue of some convolutional_layer issue? Don't worry, it is not you!

Thats because the combination of CUDA, Nvidia driver, nvcc is awful. Here some tricks to fix errors (learned the hard way):

First check the following things:
* correct CUDA version for base system? Be sure to select the right CUDA installer!
* correct nvidia driver for installed CUDA version? Find out [here](https://docs.nvidia.com/deploy/cuda-compatibility/index.html)
* nvcc works and uses correct CUDA version? The 'make' command will use the CUDA version from nvcc, so just having the correct version of CUDA is not enough.

Find out the versions (and more usefull commands):
* some commands to test if your nvidia stuff works: 'nvidia-smi' , 'nvidia-settings' , 'nvcc --version'. Doesnt work?
    * one possible solution: clean up and uninstall all nvidia stuff. 'sudo apt --purge autoremove nvidia*'
* check nvidia driver version which is used: 'nvidia-smi' or 'cat /proc/driver/nvidia/version'
    * install specific nvidia driver: 'sudo apt install nvidia-driver-<...>'
    * (optinal) install settings 'sudo apt install nvidia-settings'
* check CUDA version (several ways):
    * check /usr/local/<hereAreYourCUDAs>
    * check with 'nvcc --version' (they should match, important is that your nvcc uses the version your nvidia driver supports)      
* nvcc doesn't use correct version? fix with 'export PATH=/usr/local/<yourWantedCUDA>/bin:$PATH'
* nvcc doesn't work? Try 'sudo apt install nvidia-cuda-toolkit'

Also: don't forget rebooting after uninstalling/installing drivers or CUDA!

## also some OpenCV stuff...

Use nothing else than the source installation. Follow the instructions (cloning, cmake, make) and you shold be good to go.

One tip: use the last make for OpenCV like this 'make -j7'. This runs 7 jobs in parallel and increases build time. You will want this, believe me.

If something isn't working during installing try to fix these errors. One tip from us to our beloved anaconda users. Make sure that you don't mix up your python versions. Better not use the anaconda one.
