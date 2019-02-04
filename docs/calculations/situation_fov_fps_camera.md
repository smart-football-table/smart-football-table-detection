# Situation: Sichtfeld

Gestellhoehe h = 540mm oder 620mm (je nach Aufstellung). <br>
Kickerfeld Breite b * Laenge l --> b = 680mm, l = 1195mm. <br>

Berechnung des benoetigten Kamerasichtfeld-Winkel alpha (diagonal) mit diesen Werten:

| | h = 540mm | h = 620mm |
| --- | --- | --- |
| ganzes Feld | 104° | 96° |
| halbes Feld | 80° | 72° |

Welchen alpha haben die aktuellen Kameras (gemessen):

Kinect V1 - 76° <br>
Logitech C270 - 54°

also:

1. Kamera mit hoeherem Alpha
2. Gestellhoehe h anpassen
* auf 881mm fuer Kindect V1 (wenn ganzes Feld)
* auf 580mm fuer Kinect V1 (wenn halbes Feld, dafuer zwei Cams)
* auf 1350mm fuer Logitech C270 (wenn ganzes Feld)
* auf 889mm fuer Loigtech C270 (wenn halbes Feld, dafuer zwei Cams)

<hr>

# Situation: Bilder pro Sekunde

Maximale Ballgeschwindigkeit laut SAP bei 45 km/h, laut diverser Foren um die 60 km/h.

Annahme hier: 50,4 km/h, also 14000 mm/s.

Bdeutet: Ball bewegt sich zwischen zwei Frames...

* ... bei 30fps (Standard-Webcam) um 466,6mm weiter.
* ... bei 60fps um 233,3mm weiter.
* ... bei 90fps um 155,5mm weiter.
* ... bei 120fps um 116,6mm weiter.
* ... bei 240fps (GoPro Hero7 Black) um 58,3mm weiter.
* ... bei 350fps (SAP Leonardo, eigene Angabe) um 40mm weiter.


Empfohlene Maße beim Kickerbau: 1200mm Spielfeldlaenge, 8 Stangen, zwischen jeder Stange 150mm, Torwartstange zu Wand 75mm. <br>

Bedeutet: Strecke zwischen Stuermerstange und Tor sind 375mm. Um Schussgeschwindigkeit bei Torschuss festzustellen werden also mindestens **37,3fps** benoetigt.

Aber: kuerzeste Schussstrecke ist von Stange A bis zur Nachbarstange bei Block (unter der Annahme der Torhueter schießt nicht rueckwaerts an die Wand), also 150mm. Hier werden also mindestens **93,3fps** benoetigt.

<hr>

# Resultat: Anforderungen an Kamera

#### Bestenfalls: Sichtfeld = >96° & Framerate = 90fps

Moeglichkeiten (noch zu pruefen):

### vorhandene Kameras:

#### Kinect V1 (and V2)

* Sichtfeld = 76°
* Framerate = zwischen 15fps und 30fps

[Precision of Depth Sensor](https://stackoverflow.com/questions/7696436/precision-of-the-kinect-depth-camera) <br>
[Pretty good detection of ball](https://vvvv.org/contribution/kinect-hitboxes-dx11) --> [video](https://www.youtube.com/watch?v=I9TyfeeTKFk&feature=youtu.be&t=32) <br>
[Precision Comparison V1 and V2, also some good details](https://www.dfki.de/fileadmin/user_upload/import/8767_wasenmuller2016comparison.pdf) <br>
[Complete Comparison V1 and V2](https://www.researchgate.net/publication/299132365/figure/tbl1/AS:613865867989009@1523368436499/Comparison-between-Kinect-v1-and-Kinect-v2.png)

###### Notiz: man muesste definitv Gestellhoehe h anpassen, außerdem sollte ueber die Framerate nachgedacht werden. Unsicher ist auch, welchen Mehrwert die Tiefenerkennung bietet.

#### Logitech C270

* Sichtfeld = 54°
* Framerate = von Haus aus 15fps, scheinbar aber softwareseitig (und laut Hersteller) auf 30fps aufskalierbar

###### Notiz: faellt fast zu hunderprozent aus der Betrachtung raus

### Kameras, die gekauft werden muessten:

#### GoPro Hero 7 Black

* Sichtfeld = 150° (je nach Modus)
* Framerate = 240fps (maxial im Slowmo-Modus)
* Kostenpunkt ca. 400€

[GoPro-API Python](https://pypi.org/project/goprocam/) <br>
[Use GoPro with OpenCV](https://stackoverflow.com/questions/36112313/how-connect-my-gopro-hero-4-camera-live-stream-to-opencv-using-python) <br>
[GoPro Hero 7 FoV](https://gopro.com/help/articles/question_answer/hero7-field-of-view-fov-information) <br>

###### Notiz: folgende Fragen muessten beantwortet werden: kann die GoPro ohne Probleme angesteuert werden (siehe Links) und braucht man fuer die erhoehte Datenmenge mehr Leistung bzw. kann ueberhaupt mit maximalen Einstellungen uebertragen werden.

#### Genius F100 Widecam

* Sichtfeld = 120°
* Framerate = 30fps
* Kostenpunkt ca. 50€

[Amazon](https://www.amazon.de/Genius-32200213101-F100-Widecam/dp/B0080CE5M4/ref=sr_1_fkmr0_1?ie=UTF8&qid=1549121342&sr=8-1-fkmr0&keywords=Genius+120-degree+Ultra+Wide+Angle+Full+HD+Conference+Webcam%28WideCam+F100%29)

###### Notiz: an sich ja gutes Sichtfeld, aber schlechte Bewertungen im Bezug auf Fokus und Framerate. Dafuer guenstig.

### Vergleiche/Tests:

[All Top Logitech Cams](https://addpipe.com/blog/top-logitech-webcams-compared/)
