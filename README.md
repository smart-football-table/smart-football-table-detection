# Smart football table Detection

Using OpenCV to detect ball on gamefield. ADD MORE.

## MQTT messages
| topic                      | Description                                   | Example payload        |
| -------------------------- | --------------------------------------------- |----------------------- |
| leds/backgroundlight/color | Sets the background light, default is #000000 | #CC11DD                |
| game/score                 | The teams' scores                             | { "score": [ 0, 3 ] }  |
| game/foul                  | Some foul has happened                        | -                      |
| game/gameover              | A match ended                                 | { "winners": [ 0 ] }   |
| game/idle                  | Is there action on the table                  | { "idle": true }       |
| leds/foregroundlight/color | Foreground light overrules everything else if not #000000 | #111111    |

## Problems and Bad Stuff

* Java part isn't an image
* adjustment needs hardware (mouse, keyboard)
* adjustment output in txt (not for long-term)
* no adjustment for different angle of cameras
* Filewriter without test
* Mqtt without test
* some tests are "ignore"
* camera 1 is left camera (hardcode)
* there are two cameras (hardcode)
* ballPositionHandler looks awful
* JSON mqtt is hardcoded
* adjustment has a bad tests
* pythoncode of adjustment could look better

## What to do next

* try out hough circles
* try out yolov3
* compare results and increase recognition
* Fix Problems
* adjustment needs a "color only" mode
* control start of adjustment via MQTT
* send positions from opencvhandler is bad with buffer
* add more features
