# Testbilder

`generated-test-photo.jpg` (300x367) ist selbst erzeugt und zeigt keine Person. Der Befehl schreibt die Datei byte-gleich neu:

```bash
magick -seed 7 -size 300x367 plasma:skyblue-forestgreen -blur 0x2 -fill "#f4d35e" -draw "circle 220,70 220,100" -fill "#3d5a40" -draw "polygon 0,367 0,250 90,190 170,260 240,210 300,240 300,367" -strip -quality 90 generated-test-photo.jpg
```

`ImageConverterTest` liest die Datei und prüft die Größe 300x367.
