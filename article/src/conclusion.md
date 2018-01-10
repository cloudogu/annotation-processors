## Fazit

An den gezeigten Beispielen lässt sich sehr gut erkennen, das Annotation Prozessoren ein sehr mächtiges Werkzeug sind. Annotation Prozessoren können, wie auf magische Weise, Quellcode und Konfigurationsdateien erzeugen. 

Aber mit großer Macht kommt auch große Verantwortung einher. Wenn man nur den Quellcode eines Projektes betrachtet, fehlen die generierten Dateien. Diese tauchen erst nach dem Kompilieren auf und auch dann kann man auf den ersten Blick nicht erkennen, woher die generierten Dateien kommen und wie sie erzeugt wurden.

Deshalb empfehlt es sich in der Projekt Dokumentation darauf hinzuweisen das Dateien beim kompilieren erzeugt werden und in denn Kommentaren der generierten Dateien sollte man auf den Annotation Prozessor verweisen der sie erzeugt hat. Für Java-Quellcode gibt es zudem eine spezielle Annotation für generierte Klassen ([@Generated](https://docs.oracle.com/javase/7/docs/api/javax/annotation/Generated.html)), die man verwenden kann um auf dessen Herkunft zu verweisen.

Alle Beispiele und der Quellcode des Artikels, steht bei [Github](https://github.com/cloudogu/annotation-processors) unter der MIT-Lizenz zur Verfügung.