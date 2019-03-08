# Situation: Field of View (FoV)

Height h of our camera = 540mm or 620mm. <br>
football table weidth w * length l --> w = 680mm, l = 1195mm. <br>

Calculation of neccessary camera field of view angle alpha (diagonal):

| | h = 540mm | h = 620mm |
| --- | --- | --- |
| whole field | 104° | 96° |
| half of the field | 80° | 72° |

# Situation: Frames per second (FPS)

What is the hightest speed the ball can achive?

Max velocity is 45 km/h (information from SAP Leonardo), searching in the internet results in 60 km/h.

We expect: 50,4 km/h, which means 14000 mm/s.

So when assuming different fps-values. the ball moves...

* ... 466,6mm between two frames with 30fps (a normal webcam).
* ... 233,3mm between two frames with 60fps.
* ... 155,5mm between two frames with 90fps.
* ... 116,6mm between two frames with 120fps.
* ... 58,3mm between two frames with 240fps (GoPro Hero7 Black).
* ... 40mm between two frames with 350fps (SAP Leonardo System).


Values of a football table are standardized: 1200mm lenght of gamefield, 8 kicker poles, between each pole 150mm, goalkeeperpole <-> wall is 75mm. <br>

This means: To get the velocity of the shot between striker pole and the goal (375mm) we need **37,3fps**.

But: To get the velocity of the shortest possible shot (between two poles, 150mm) we need **93,3fps**. (To be honest: the shortest possible shot is goalkeeper pole <-> wall with 75mm, but we expect this not to happen without purpose)

<hr>

# Result: Requirements for camera

### camera should have fov >= 96° & fps >= 90fps

### Finding out the field of view if not named in camera data sheet:

1) Find out sensor type (for exampe 1/2.7)
2) Find out crop factor (also called focal length mulitplier) for this sensor. Use the [Wikipedia page](https://en.wikipedia.org/wiki/Image_sensor_format) for this.
3) Find out lens type and the lens focal lengt (normally in mm, something like 2.1mm)
4) Use Max Lyons [calculator](http://www.tawbaware.com/maxlyons/calc.htm) to calculate the field of view.
Moeglichkeiten (noch zu pruefen):

### some possible ideas:

#### Kinect V1 (and V2)

* Field of View = 76°
* Framerate = between 15fps and 30fps

[Precision of Depth Sensor](https://stackoverflow.com/questions/7696436/precision-of-the-kinect-depth-camera) <br>
[Pretty good detection of ball](https://vvvv.org/contribution/kinect-hitboxes-dx11) --> [video](https://www.youtube.com/watch?v=I9TyfeeTKFk&feature=youtu.be&t=32) <br>
[Precision Comparison V1 and V2, also some good details](https://www.dfki.de/fileadmin/user_upload/import/8767_wasenmuller2016comparison.pdf) <br>
[Complete Comparison V1 and V2](https://www.researchgate.net/publication/299132365/figure/tbl1/AS:613865867989009@1523368436499/Comparison-between-Kinect-v1-and-Kinect-v2.png)

#### Logitech C270

* Field of View = 54°
* Framerate = 15fps

#### GoPro Hero 7 Black

* Field of View = 150° (depends on mode)
* Framerate = 240fps (max)
* Kostenpunkt ca. 400€

[GoPro-API Python](https://pypi.org/project/goprocam/) <br>
[Use GoPro with OpenCV](https://stackoverflow.com/questions/36112313/how-connect-my-gopro-hero-4-camera-live-stream-to-opencv-using-python) <br>
[GoPro Hero 7 FoV](https://gopro.com/help/articles/question_answer/hero7-field-of-view-fov-information) <br>

###### Note: high latency when using ffmpeg for streaming. Using the usb port isn't possible. When a latency isn't a problem, the go pro is good to go.

### Comparations/Tests:

[All Top Logitech Cams](https://addpipe.com/blog/top-logitech-webcams-compared/)
