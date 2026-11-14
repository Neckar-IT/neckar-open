# test-data

## Bilder von `TestData`

`src/commonMain/resources/images/drop.jpg` (1024x1024) und `src/commonMain/resources/images/drop_300.jpg` (300x300) sind selbst erzeugt. `TestData.loadDropBytes()` und `TestData.loadDrop300Bytes()` lesen sie. Die Befehle im Verzeichnis `src/commonMain/resources/images/` schreiben beide Dateien byte-gleich neu:

```bash
magick -seed 7 -size 1024x1024 plasma:navy-lightblue -blur 0x4 -fill "#e8f1f8" -draw "ellipse 512,560 230,300 0,360" -fill "#9cc3e6" -draw "ellipse 470,480 60,90 0,360" -strip -quality 90 drop.jpg
magick drop.jpg -resize 300x300 -strip -quality 90 drop_300.jpg
```
