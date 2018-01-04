# Java Annotation Prozessoren

Java Annotation Prozessoren sind ein sehr mächtiges Werkzeug im Werkzeugkasten eines Entwicklers. Mit ihnen lassen sich z.B. Informationen zur Buildzeit protokollieren, der Build kann mit einer Fehlermeldung abgebrochen werden, es können Konfigurationen und Dokumentationen erzeugt, Klassen verändert oder neu erstellt werden.

In dem ersten Abschnitt des Artikels betrachten wir die grundsätzliche Funktionsweise von Annotation Prozessoren anhand eines kleinen Beispiels. In den beiden weiteren Abschnitten werden wir uns das Erzeugen von Konfigurationen und das generieren von Code genauer ansehen. Der in diesem Artikel verwendete Code, kann unter [https://github.com/cloudogu/annotation-processors/](https://github.com/cloudogu/annotation-processors) eingesehen werden.

Annotation Prozessoren werden während des Builds vom Compiler (javac) aufgerufen, wenn eine der konfigurierten Annotationen gefunden wurde. Dabei kann ein Annotation Prozessor bestimmen, über welche Annotationen er benachrichtigt werden will. Das können eine Einzelne, mehrere oder alle sein. Wenn der Compiler eine Annotation findet, wird überprüft, ob für diese ein Prozessor registriert wurde. Ist dies der Fall, wird er ausgeführt. An diesem Punkt kann der Annotation Prozessor seine Arbeit verrichten und entscheiden, ob für die gefundene Annotation noch weitere Prozessoren aufgerufen werden dürfen.

## OpenSource Beispiele

Ein prominentes Beispiel für einen Annotation Prozessor ist der `„META-INF/services generator“` von Kohsuke Kawaguchi der aus einer `MetaInfServices` Annotation die Konfiguration für den `Java 6 ServiceLoader` erzeugen kann.

Ein weiteres Beispiel ist das `„Project Lombok“`. Es verspricht, mit einer Reihe von Annotationen, den Boilerplate Code von Java Klassen automatisch zu generieren, z.B.: Getter, Setter, HashCode oder Equals Methoden.

## Ein einfacher Annotation Prozessor

Annotation Prozessoren müssen das Interface `javax.annotation.processing.Processor` implementieren, in den meisten Fällen empfiehlt es sich die Klasse `javax.annotation.processing.AbstractProcessor` zu erweitern, da sie nützliche Hilfsmethoden enthält. Unser Beispiel Annotation Prozessor soll eine Meldung ausgeben, wenn eine bestimmte Annotation gefunden wurde. Dafür erstellen wir zuerst unsere Annotation
(`com/cloudogu/blog/annotationprocessor/log/Log.java`):

```java
package com.cloudogu.blog.annotationprocessor.log;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
public @interface Log {}
```