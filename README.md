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
