# README

### Google Fonts herunterladen:

- `gradle downloadFonts`
- `git commit`


### Symlink erstellen:

- Terminal im Zielverzeichnis öffnen
- Dann: ln -s -r ../path/../to/../actualResource

### CSS
Vorlage ist [`nit-fonts.css`](../../meistercharts/meistercharts-demos/meistercharts-demos-common/src/webMain/resources/nit-fonts.css): eine variable Font braucht neben `src: url(...) format('truetype-variations')` auch `font-weight`, `font-style` und `font-stretch` als Wertebereiche, sonst rendert der Browser Achsen ausserhalb des Könnens der Font.
