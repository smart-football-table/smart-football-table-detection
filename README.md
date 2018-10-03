# Smart football table

### TODO

* correcting the dock solution (more like improving)
* testing on table
* improving flexibility
    * frame size
    * kicker size (maybe small stickers table for detection)
    * camera
* adding and improving modules

### Ideas to implement

###### Spielerbezogen

* Siege/Niederlage
* Siege/Position
* durschn. Ballbesitz
* Tore nach Lokation
* Schussgeschwindigkeit
* gehaltene Bälle
* Siege/anderer Spieler
* Siege/Tageszeit
* [...]

###### Spielbezogen

* Ergebnis
* Ballbesitz/Seite
* Heatmap Ball
* teilnehmende Spieler, Uhrzeit, ... (Metadaten)
* Ballbesitz/Spieler
* Ballbesitz pro Spielfigur
* Tore von wo/wem
* durschn. Ballgeschw.
* gehaltene Bälle/Seite
* [...]

###### Kickerbezogen

* welche Farbe gewinnt öfter
* durchschn. Ballgesch.
* Tore von wo
* Uhrzeit/Benutzung
* Preis, Maße, ... (Metadaten)

###### außerdem

* Konfigurierkram
  * Kameragröße/-höhe und dadurch reslutierend Framegröße
  * Tischkicker hinterlegen und einstellen
  * Buttons am Tischkicker für neues Spiel (Reset und Start) und Spiel beenden (Abschließen und Auswerten)
  * LEDs oder so für visuelles Schmankerl
* eigenes Spielerkonto
  * nach einmaliger Anmeldung mittels NFC Chip am Kicker anmelden (Positionsbezogen)
  * daraus Statistiken erstellen (s.o.)

### Calculations for Camera (Logitech Webcam)

* Frame = 600px
* Camera-Angle = 33.3985° (height) & 43.6028° (width)

Base of Calculation:

Messure of screen size: 40cm\*30cm (w\*h) <br>
Messure of Camera height: 50cm <br>
Getting angle with tangens: tan(alpha_width/2) = (screensize/2)/cameraheight = 20/50 = 0.4, so alpha_width = 43.6028° <br>
Getting angle with tangens: tan(alpha_height /2) = (screensize/2)/cameraheight = 15/50 = 0.3, so alpha_height = 33.3985° <br>
