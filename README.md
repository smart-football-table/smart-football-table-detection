# Smart football table

![logo](https://github.com/KingMus/smart-football-table/blob/master/docs/logo/SFT_Logo_Color_small.png)

## Shortcut: --> [Kamera-Gedanken](https://github.com/KingMus/smart-football-table/blob/master/docs/calculations/situation_fov_fps_camera.md)

## Shortcut: --> Architecture

![arc](https://github.com/KingMus/smart-football-table/blob/master/docs/architecture/SmartFootballTable_Architecture.png)

## Shortcut: --> Kicker-Maße

![werte](https://github.com/KingMus/smart-football-table/blob/master/docs/calculations/kicker_werte.jpg)

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
* eigenes Spielerkonto
  * nach einmaliger Anmeldung mittels NFC Chip am Kicker anmelden (Positionsbezogen)
  * daraus Statistiken erstellen (s.o.)
* LED-Leiste
  * an Position des Balls heller leuchten
  * bei Regelbruch Signallicht
  * bei Ballbesitz in Teamfarbe
  * bei Torgefahr Spannungslicht
  * Visualisierung des Spielstandes
