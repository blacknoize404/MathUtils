#!/usr/bin/env python3
"""
Genera los recursos de marca de MathUtils Studio a partir del sistema de diseño de barnesinside.

    python build_assets.py [ruta-al-sistema-de-diseno]

Produce en src/main/resources/mathutils/:
  fonts/   instancias estáticas de Geist y Geist Mono (JavaFX no aplica pesos de fuentes variables)
  icons/   iconos Lucide convertidos a trazados SVG (icons.properties)
  css/     theme-light.css y theme-dark.css con los tokens como colores buscados (-bi-*)
  brand/   logos PNG

La carpeta del sistema de diseño es la fuente de verdad: nunca se modifica.
"""
import re
import shutil
import sys
from pathlib import Path

DEFAULT_SOURCE = Path(r"C:\Users\BarnesInside\Desktop\BarnésInside Web\Barnesinside - Sistema de Diseño")
RESOURCES = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "mathutils"


# --------------------------------------------------------------------------- fuentes

def build_fonts(source: Path) -> None:
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer

    out = RESOURCES / "fonts"
    out.mkdir(parents=True, exist_ok=True)

    # (archivo variable, peso, familia, estilo, nombre de salida, negrita, cursiva)
    jobs = [
        ("Geist-VariableFont_wght.ttf", 400, "Geist", "Regular", "Geist-Regular.ttf", False, False),
        ("Geist-VariableFont_wght.ttf", 700, "Geist", "Bold", "Geist-Bold.ttf", True, False),
        ("Geist-VariableFont_wght.ttf", 500, "Geist Medium", "Regular", "Geist-Medium.ttf", False, False),
        ("Geist-VariableFont_wght.ttf", 600, "Geist SemiBold", "Regular", "Geist-SemiBold.ttf", False, False),
        ("Geist-Italic-VariableFont_wght.ttf", 400, "Geist", "Italic", "Geist-Italic.ttf", False, True),
        ("GeistMono-VariableFont_wght.ttf", 400, "Geist Mono", "Regular", "GeistMono-Regular.ttf", False, False),
        ("GeistMono-VariableFont_wght.ttf", 700, "Geist Mono", "Bold", "GeistMono-Bold.ttf", True, False),
        ("GeistMono-VariableFont_wght.ttf", 500, "Geist Mono Medium", "Regular", "GeistMono-Medium.ttf", False, False),
    ]

    for filename, weight, family, style, target, bold, italic in jobs:
        font = TTFont(source / "assets" / "fonts" / filename)
        static = instancer.instantiateVariableFont(font, {"wght": weight}, inplace=False)

        full = family if style == "Regular" else f"{family} {style}"
        postscript = full.replace(" ", "")
        names = static["name"]
        for name_id in (1, 2, 3, 4, 6, 16, 17, 21, 22, 25):
            names.removeNames(nameID=name_id)
        for platform, encoding, language in ((3, 1, 0x409), (1, 0, 0)):
            names.setName(family, 1, platform, encoding, language)
            names.setName(style, 2, platform, encoding, language)
            names.setName(f"{postscript}-{style};static", 3, platform, encoding, language)
            names.setName(full, 4, platform, encoding, language)
            names.setName(f"{postscript}-{style}", 6, platform, encoding, language)

        os2 = static["OS/2"]
        os2.usWeightClass = weight
        selection = os2.fsSelection & ~(1 | 32 | 64)
        selection |= 32 if bold else 0
        selection |= 1 if italic else 0
        selection |= 64 if (not bold and not italic) else 0
        os2.fsSelection = selection
        static["head"].macStyle = (1 if bold else 0) | (2 if italic else 0)

        static.save(out / target)
        print("fuente", target)


# --------------------------------------------------------------------------- iconos

ICONS = [
    "Calculator", "Network", "ListOrdered", "Sigma", "ChartLine", "Sun", "Moon", "Copy", "Trash2", "History",
    "Search", "Play", "Pause", "ChevronRight", "ChevronDown", "ChevronLeft", "Delete", "Check", "X", "Info",
    "TriangleAlert", "CircleAlert", "ArrowRight", "ZoomIn", "ZoomOut", "Maximize", "Download", "Languages",
    "Settings", "Variable", "SquareFunction", "Binary", "LayoutGrid", "Workflow", "Equal", "Plus", "Minus",
    "Divide", "Pi", "Radical", "Superscript", "Infinity", "Percent", "Brackets", "SkipBack", "SkipForward",
    "RotateCcw", "Eraser", "FileText", "Braces", "Share2", "Sparkles", "Lightbulb", "Hash",
    "Image", "Code", "Terminal", "BookOpen", "CornerDownLeft", "Undo2", "Pointer",
]


def attr(attrs: str, name: str, default=None):
    match = re.search(r"\b" + name + r':\s*"([^"]*)"', attrs)
    return match.group(1) if match else default


def number(value: str) -> float:
    return float(value)


def fmt(value: float) -> str:
    text = f"{value:.4f}".rstrip("0").rstrip(".")
    return text if text else "0"



PATH_TOKEN = re.compile(r"([MmLlHhVvCcSsQqTtAaZz])|(-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?)")
PATH_ARITY = {"M": 2, "L": 2, "H": 1, "V": 1, "C": 6, "S": 4, "Q": 4, "T": 2, "A": 7, "Z": 0}


def normalize_path(d: str) -> str:
    """Expande los comandos implícitos (m6 9 6 6 -> m6 9 l6 6): JavaFX no los interpreta bien."""
    tokens = [(command or number, bool(command)) for command, number in PATH_TOKEN.findall(d)]
    out = []
    i = 0
    while i < len(tokens):
        token, is_command = tokens[i]
        if not is_command:
            i += 1
            continue
        command = token
        i += 1
        arity = PATH_ARITY[command.upper()]
        if arity == 0:
            out.append(command)
            continue
        first = True
        while i < len(tokens) and not tokens[i][1]:
            args = [tokens[i + k][0] for k in range(arity) if i + k < len(tokens) and not tokens[i + k][1]]
            if len(args) < arity:
                break
            current = command
            if not first and command in "Mm":
                current = "L" if command == "M" else "l"
            out.append(current + " ".join(args))
            i += arity
            first = False
    return " ".join(out)


def to_path(tag: str, attrs: str) -> str | None:
    if tag == "path":
        return normalize_path(attr(attrs, "d"))
    if tag == "line":
        x1, y1, x2, y2 = (number(attr(attrs, n)) for n in ("x1", "y1", "x2", "y2"))
        return f"M{fmt(x1)} {fmt(y1)}L{fmt(x2)} {fmt(y2)}"
    if tag in ("circle", "ellipse"):
        cx, cy = number(attr(attrs, "cx")), number(attr(attrs, "cy"))
        rx = number(attr(attrs, "rx", attr(attrs, "r")))
        ry = number(attr(attrs, "ry", attr(attrs, "r")))
        return (f"M{fmt(cx - rx)} {fmt(cy)}A{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(cx + rx)} {fmt(cy)}"
                f"A{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(cx - rx)} {fmt(cy)}Z")
    if tag == "rect":
        x, y = number(attr(attrs, "x", "0")), number(attr(attrs, "y", "0"))
        w, h = number(attr(attrs, "width")), number(attr(attrs, "height"))
        r = min(number(attr(attrs, "rx", "0")), w / 2, h / 2)
        if r == 0:
            return f"M{fmt(x)} {fmt(y)}H{fmt(x + w)}V{fmt(y + h)}H{fmt(x)}Z"
        return (f"M{fmt(x + r)} {fmt(y)}H{fmt(x + w - r)}A{fmt(r)} {fmt(r)} 0 0 1 {fmt(x + w)} {fmt(y + r)}"
                f"V{fmt(y + h - r)}A{fmt(r)} {fmt(r)} 0 0 1 {fmt(x + w - r)} {fmt(y + h)}"
                f"H{fmt(x + r)}A{fmt(r)} {fmt(r)} 0 0 1 {fmt(x)} {fmt(y + h - r)}"
                f"V{fmt(y + r)}A{fmt(r)} {fmt(r)} 0 0 1 {fmt(x + r)} {fmt(y)}Z")
    if tag in ("polyline", "polygon"):
        points = attr(attrs, "points").replace(",", " ").split()
        pairs = [(points[i], points[i + 1]) for i in range(0, len(points) - 1, 2)]
        path = "M" + "L".join(f"{a} {b}" for a, b in pairs)
        return path + ("Z" if tag == "polygon" else "")
    return None


def extract_block(js: str, name: str):
    """Contenido del arreglo `const Name = [ ... ];` equilibrando corchetes (una o varias líneas)."""
    match = re.search(r"const " + re.escape(name) + r" = \[", js)
    if not match:
        return None
    start = match.end()
    depth = 1
    i = start
    in_string = False
    while i < len(js) and depth:
        c = js[i]
        if c == '"' and js[i - 1] != "\\":
            in_string = not in_string
        elif not in_string:
            if c == "[":
                depth += 1
            elif c == "]":
                depth -= 1
        i += 1
    return js[start:i - 1]


def build_icons(source: Path) -> None:
    js = (source / "assets" / "vendor" / "lucide.js").read_text(encoding="utf-8")
    out = RESOURCES / "icons"
    out.mkdir(parents=True, exist_ok=True)

    lines = ["# Iconos Lucide (ISC) convertidos a trazados SVG de 24x24. Un icono por línea: nombre=trazo|trazo|..."]
    missing = []
    for name in ICONS:
        body = extract_block(js, name)
        if body is None:
            missing.append(name)
            continue
        paths = []
        for tag, attrs in re.findall(r'\[\s*"(\w+)",\s*\{(.*?)\}\s*\]', body, re.S):
            path = to_path(tag, attrs)
            if path:
                paths.append(re.sub(r"\s+", " ", path.strip()))
        key = re.sub(r"(?<!^)(?=[A-Z0-9])", "-", name).lower()
        lines.append(f"{key}={'|'.join(paths)}")
    (out / "icons.properties").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"iconos: {len(lines) - 1} generados" + (f", no encontrados: {missing}" if missing else ""))


# --------------------------------------------------------------------------- tema

def parse_tokens(source: Path) -> dict:
    tokens = {}
    text = (source / "tokens" / "colors.css").read_text(encoding="utf-8")
    for name, value in re.findall(r"--(color-[\w-]+):\s*([^;]+);", text):
        tokens.setdefault(name, value.strip())
    return tokens


def hex_to_rgb(value: str):
    value = value.lstrip("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)


def rgba(rgb, alpha: float) -> str:
    return f"rgba({rgb[0]}, {rgb[1]}, {rgb[2]}, {alpha})"


def build_theme(source: Path) -> None:
    t = parse_tokens(source)
    out = RESOURCES / "css"
    out.mkdir(parents=True, exist_ok=True)

    def hexcolor(name):
        return t[name].upper()

    themes = {
        "light": dict(bg=hexcolor("color-white"), fg=hexcolor("color-black"), border=hexcolor("color-gray-200"),
                      elevated=hexcolor("color-white"), secondary=hexcolor("color-gray-50"),
                      tertiary=hexcolor("color-gray-100")),
        "dark": dict(bg=hexcolor("color-black"), fg=hexcolor("color-white"), border=hexcolor("color-gray-700"),
                     elevated=hexcolor("color-gray-800"), secondary=hexcolor("color-gray-900"),
                     tertiary=hexcolor("color-gray-800")),
    }

    accents = ["violet", "cyan", "magenta", "amber", "red", "emerald"]

    for name, theme in themes.items():
        fg = hex_to_rgb(theme["fg"])
        bg = hex_to_rgb(theme["bg"])
        lines = [
            f"/* Generado por tools/build_assets.py desde el sistema de diseño barnesinside. No editar a mano. */",
            ".root {",
            f"    -bi-bg: {theme['bg']};",
            f"    -bi-fg: {theme['fg']};",
            f"    -bi-muted: {hexcolor('color-gray-400')};",
            f"    -bi-border: {theme['border']};",
            f"    -bi-bg-elevated: {theme['elevated']};",
            f"    -bi-bg-secondary: {theme['secondary']};",
            f"    -bi-bg-tertiary: {theme['tertiary']};",
            f"    -bi-accent: {hexcolor('color-mint')};",
            f"    -bi-accent-hover: {hexcolor('color-mint-dim')};",
            f"    -bi-accent-bg: {rgba(hex_to_rgb(hexcolor('color-mint')), 0.1)};",
            f"    -bi-on-accent: {hexcolor('color-black')};",
        ]
        for pct in (5, 10, 15, 50, 60, 70, 80, 90):
            lines.append(f"    -bi-fg-{pct}: {rgba(fg, pct / 100)};")
        for pct in (70, 80, 90):
            lines.append(f"    -bi-bg-{pct}: {rgba(bg, pct / 100)};")
        for accent in accents:
            lines.append(f"    -bi-{accent}: {hexcolor('color-' + accent)};")
            lines.append(f"    -bi-{accent}-dim: {hexcolor('color-' + accent + '-dim')};")
            lines.append(f"    -bi-{accent}-bg: {rgba(hex_to_rgb(hexcolor('color-' + accent)), 0.1)};")
        lines += [
            f"    -bi-success: {hexcolor('color-mint')};",
            f"    -bi-warning: {hexcolor('color-amber')};",
            f"    -bi-error: {hexcolor('color-red')};",
            f"    -bi-info: {hexcolor('color-cyan')};",
            "}",
            "",
        ]
        (out / f"theme-{name}.css").write_text("\n".join(lines), encoding="utf-8")
        print("tema", name)


# --------------------------------------------------------------------------- logos

def copy_logos(source: Path) -> None:
    out = RESOURCES / "brand"
    out.mkdir(parents=True, exist_ok=True)
    for png in (source / "assets" / "logos" / "png").glob("*@5x.png"):
        shutil.copy(png, out / png.name.replace("@5x", ""))
        print("logo", png.name)


if __name__ == "__main__":
    base = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SOURCE
    if not base.exists():
        sys.exit(f"No existe la carpeta del sistema de diseño: {base}")
    build_fonts(base)
    build_icons(base)
    build_theme(base)
    copy_logos(base)
