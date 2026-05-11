# Thesis LaTeX Project

## Struktura souborů

```
thesis/
├── main.tex       — hlavní soubor (preamble, titulní strana, front matter)
├── chapters.tex   — tělo práce (automaticky konvertováno z Word dokumentu)
└── README.md      — tento soubor
```

## Kompilace

### Instalace LaTeX (macOS)
```bash
brew install --cask mactex
```
nebo stáhnout z https://www.tug.org/mactex/ (cca 4 GB, zahrnuje vše)

### Rychlá kompilace
```bash
pdflatex main.tex
pdflatex main.tex   # druhý průchod pro TOC a reference
```

### Pro správné citace (pokud přidáte .bib soubor)
```bash
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

## Co je potřeba doplnit v main.tex

Hledejte `%% TODO:` nebo `%% <-- DOPLŇTE`:

1. **Řádek 85** — `\ThesisAuthor` — vaše jméno a příjmení
2. **Řádek 86** — `\ThesisSupervisor` — jméno + tituly vedoucího práce
3. **Řádek 88** — `\ThesisDepartment` — název katedry
4. **Řádek 89** — `\ThesisDate` — měsíc a rok odevzdání
5. **Řádek 118** — logo ČVUT — odkomentujte `\includegraphics` a vložte `logo_cvut.pdf`
6. **Abstrakt CZ** — doplňte/upravte text abstraktu (~200 slov)
7. **Abstract EN** — přeložte abstrakt do angličtiny
8. **Poděkování** — upravte text
9. **Příloha A** — vložte scan zadání práce

## Citace v textu

Nyní jsou citace jako plain text (bez odkazu). Pro funkční citace \cite{} doplňte do chapters.tex
reference ve tvaru `\cite{ref1}` namísto plain textu.

Příklad:
```latex
Podle interních materiálů firmy~\cite{ref1} byl zaveden...
```

## Formátování dle CTUstyle

Šablona implementuje:
- ✅ Papír A4
- ✅ Písmo Latin Modern 11pt
- ✅ Vnitřní okraj 30 mm (pro pevnou vazbu)
- ✅ Oboustranná sazba
- ✅ Modrá barva nadpisů (#0065BD = Pantone 300 C)
- ✅ Kolontituly (záhlaví: kapitola, zápatí: číslo stránky)
- ✅ Římské číslování úvodních stran, arabské od kap. 1
- ✅ Obsah (TOC)
- ✅ 1,5-násobné řádkování
- ✅ Bibliografie v thebibliography (22 záznamů)
- ✅ Přílohy (appendix)
