# Smart football table

![logo](https://github.com/KingMus/smart-football-table/blob/master/SFT_Logo.png)


### Calculations for Camera (Logitech Webcam)

* Frame = 600px
* Camera-Angle = 33.3985° (height) & 43.6028° (width)

Base of Calculation:

Messure of screen size: 40cm\*30cm (w\*h) <br>
Messure of Camera height: 50cm <br>
Getting angle with tangens: tan(alpha_width/2) = (screensize/2)/cameraheight = 20/50 = 0.4, so alpha_width = 43.6028° <br>
Getting angle with tangens: tan(alpha_height /2) = (screensize/2)/cameraheight = 15/50 = 0.3, so alpha_height = 33.3985° <br>

### Ideas to implement

* Konfigurierkram
  * Kameragröße/-höhe und dadurch reslutierend Framegröße
  * Tischkicker hinterlegen und einstellen
  * Buttons am Tischkicker für neues Spiel (Reset und Start) und Spiel beenden (Abschließen und Auswerten)
  * LEDs oder so für visuelles Schmankerl
* eigenes Spielerkonto
  * nach einmaliger Anmeldung mittels NFC Chip am Kicker anmelden (Positionsbezogen)
  * daraus Statistiken im Zusammenhang zu Kicker, Position, Farbe, Gegner, Uhrzeit, Teammitglied erstellen (...)
* mögliche Auswertungsformen
  * Heatmaps (wo ist Ball während Spiel am meisten)
  * Prozentualer Ballbesitzt pro Spielfigur
  * Tore von wo und wem
  * ...
  * alles im Bezug auf Spielerkonto
