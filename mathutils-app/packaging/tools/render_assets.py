"""Renderiza desde los SVG de la marca los iconos y las imágenes del instalador.

Usa un Chromium sin interfaz (Edge o Chrome) para dibujar los SVG a cada tamaño exacto, de modo que
los iconos pequeños salen nítidos en vez de reducirse desde uno grande. Genera en ``packaging/assets``:

- ``icons/mathutils-studio-<n>.png`` (16 a 512), ``mathutils-studio.ico`` y ``mathutils-studio.svg``
- ``installer/wizard-<ancho>.bmp`` y ``installer/small-<lado>.bmp`` para Inno Setup
- ``installer/uninstall.ico`` es el mismo icono de la aplicación

Uso::

    python packaging/tools/render_assets.py [carpeta del sistema de diseño]

Los resultados se guardan en el repositorio, así que solo hace falta ejecutarlo si cambia la marca.
"""
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
PACKAGING = HERE.parent
ASSETS = PACKAGING / "assets"
BRAND = PACKAGING / "brand"
FONTS = PACKAGING.parent / "src" / "main" / "resources" / "mathutils" / "fonts"
DEFAULT_SOURCE = Path.home() / "Desktop" / "BarnésInside Web" / "Barnesinside - Sistema de Diseño"

BROWSERS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    "/usr/bin/google-chrome",
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
]

PROFILE = tempfile.mkdtemp(prefix="render-profile-")
ICON_SIZES = [16, 24, 32, 48, 64, 128, 256, 512]
WIZARD_WIDTHS = [(164, 314), (192, 366), (246, 459), (328, 628)]
SMALL_SIDES = [55, 64, 80, 96, 110]


def find_browser() -> str:
    for candidate in BROWSERS:
        if Path(candidate).exists():
            return candidate
    found = shutil.which("chromium") or shutil.which("google-chrome") or shutil.which("msedge")
    if found:
        return found
    sys.exit("No encuentro Edge, Chrome ni Chromium para renderizar los SVG.")


def render(browser: str, html: str, width: int, height: int, scale: float, out: Path) -> None:
    """Dibuja ``html`` en una ventana de width x height y guarda la captura con la escala dada."""
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
        page = Path(tmp) / "page.html"
        page.write_text(html, encoding="utf-8")
        shot = Path(tmp) / "shot.png"
        subprocess.run([
            browser, "--headless", "--disable-gpu", "--hide-scrollbars", "--no-sandbox",
            f"--user-data-dir={PROFILE}",
            "--default-background-color=00000000", f"--force-device-scale-factor={scale}",
            f"--window-size={width},{height}", f"--screenshot={shot}", page.as_uri(),
        ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
        # Algunos Chromium devuelven el control antes de terminar de escribir la captura
        deadline = time.time() + 30
        while (not shot.exists() or shot.stat().st_size == 0) and time.time() < deadline:
            time.sleep(0.2)
        time.sleep(0.2)
        image = Image.open(shot).convert("RGBA")
        target = (round(width * scale), round(height * scale))
        if image.size != target:
            image = image.crop((0, 0, *target)) if image.width >= target[0] and image.height >= target[1] \
                else image.resize(target, Image.LANCZOS)
        out.parent.mkdir(parents=True, exist_ok=True)
        image.save(out)


# --------------------------------------------------------------------------- páginas

def inline_svg(name: str, css: str, fill: str = "#fff") -> str:
    """El SVG de la marca incrustado en la página (sin metadatos) y con el relleno indicado.

    Los logos traen sus formas con una clase y sin color, así que como imagen saldrían negros.
    """
    raw = (BRAND / name).read_text(encoding="utf-8")
    raw = re.sub(r"<\?xml[^>]*\?>", "", raw)
    raw = re.sub(r"<metadata>.*?</metadata>", "", raw, flags=re.S)
    raw = re.sub(r"<defs>.*?</defs>", "", raw, flags=re.S)
    raw = raw.replace("<svg ", f'<svg style="{css}" ', 1)
    return raw.replace("</svg>", f"<style>path,polygon,rect{{fill:{fill}}}</style></svg>")


def icon_html(size: int) -> str:
    radius = round(size * 0.225)
    height = round(size * 0.52)
    return f"""<!doctype html><meta charset="utf-8"><style>
html,body {{ margin:0; width:{size}px; height:{size}px; background:transparent; overflow:hidden; }}
.tile {{ width:{size}px; height:{size}px; border-radius:{radius}px; background:#000; display:flex;
  align-items:center; justify-content:center; box-sizing:border-box; border:{max(1, size // 64)}px solid #2D2D2D; }}
</style><div class="tile">{inline_svg("barnesinside_isotipo_blanco.svg", f"height:{height}px;display:block")}</div>"""


def font_faces() -> str:
    faces = []
    for name, file, weight in (("Geist", "Geist-Regular.ttf", 400), ("Geist", "Geist-Medium.ttf", 500),
                               ("Geist", "Geist-SemiBold.ttf", 600), ("Geist Mono", "GeistMono-Regular.ttf", 400)):
        faces.append(f"@font-face {{ font-family:'{name}'; font-weight:{weight}; src:url('{(FONTS / file).as_uri()}'); }}")
    return "\n".join(faces)


def wizard_html(width: int, height: int, version: str) -> str:
    isotipo = inline_svg("barnesinside_isotipo_blanco.svg", "height:34px;display:block")
    imagotipo = inline_svg("barnesinside_imagotipo_blanco.svg", "width:112px;display:block;margin-top:10px;opacity:.9")
    return f"""<!doctype html><meta charset="utf-8"><style>
{font_faces()}
html,body {{ margin:0; width:{width}px; height:{height}px; background:#000; overflow:hidden; color:#fff;
  font-family:'Geist', sans-serif; }}
.panel {{ position:relative; width:{width}px; height:{height}px; box-sizing:border-box; padding:26px 20px;
  display:flex; flex-direction:column; }}
h1 {{ margin:26px 0 0; font-size:22px; line-height:1.15; font-weight:600; letter-spacing:-0.02em; }}
p {{ margin:12px 0 0; font-size:11.5px; line-height:1.45; color:#A3A3A3; }}
.rule {{ width:28px; height:2px; background:#00FF88; margin-top:18px; }}
.spacer {{ flex:1; }}
.version {{ font-family:'Geist Mono', monospace; font-size:10px; color:#737373; letter-spacing:0.04em; }}
.edge {{ position:absolute; right:0; top:0; bottom:0; width:1px; background:#2D2D2D; }}
</style><div class="panel">
{isotipo}
<h1>MathUtils<br>Studio</h1>
<div class="rule"></div>
<p>Calcula, mira el árbol y entiende cada paso.</p>
<div class="spacer"></div>
<div class="version">v{version}</div>
{imagotipo}
<div class="edge"></div></div>"""


def small_html(side: int) -> str:
    return icon_html(side)


# --------------------------------------------------------------------------- salidas

def write_ico(images: list[tuple[int, Path]], out: Path) -> None:
    """ICO con una entrada PNG por tamaño (formato aceptado desde Windows Vista)."""
    entries = []
    for size, path in images:
        data = path.read_bytes()
        entries.append((size, data))
    header = struct.pack("<HHH", 0, 1, len(entries))
    offset = 6 + 16 * len(entries)
    directory = b""
    payload = b""
    for size, data in entries:
        directory += struct.pack("<BBBBHHII", size % 256, size % 256, 0, 0, 1, 32, len(data), offset + len(payload))
        payload += data
    out.write_bytes(header + directory + payload)


def flatten_to_bmp(png: Path, bmp: Path) -> None:
    image = Image.open(png).convert("RGBA")
    background = Image.new("RGB", image.size, (0, 0, 0))
    background.paste(image, mask=image.split()[3])
    background.save(bmp, format="BMP")


def icon_svg() -> str:
    """El icono vectorial: el isotipo blanco sobre un cuadrado negro de esquinas redondeadas."""
    raw = (BRAND / "barnesinside_isotipo_blanco.svg").read_text(encoding="utf-8")
    start = raw.index("<svg")
    body = raw[start:]
    body = re.sub(r"<metadata>.*?</metadata>", "", body, flags=re.S)
    body = re.sub(r"<defs>.*?</defs>", "", body, flags=re.S)
    inner = body[body.index(">") + 1:body.rindex("</svg>")]
    inner = '<g fill="#fff">' + inner + "</g>"
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" width="128" height="128">'
            '<rect x="0.5" y="0.5" width="127" height="127" rx="28.8" fill="#000" stroke="#2D2D2D"/>'
            '<svg x="43.4" y="30.5" width="41.2" height="67" viewBox="0 0 17.9 26.16" preserveAspectRatio="xMidYMid meet">'
            f'{inner}</svg></svg>\n')


def main() -> None:
    source = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SOURCE
    logos = source / "assets" / "logos"
    BRAND.mkdir(parents=True, exist_ok=True)
    for name in ("barnesinside_isotipo_blanco.svg", "barnesinside_imagotipo_blanco.svg"):
        if (logos / name).exists():
            shutil.copy(logos / name, BRAND / name)
    if not (BRAND / "barnesinside_isotipo_blanco.svg").exists():
        sys.exit(f"Faltan los logos de la marca en {logos}")

    browser = find_browser()
    version = "0.6"
    pom = PACKAGING.parent.parent / "pom.xml"
    if pom.exists():
        match = re.search(r"<version>([\d.]+)", pom.read_text(encoding="utf-8"))
        if match:
            version = match.group(1)

    icons = ASSETS / "icons"
    for size in ICON_SIZES:
        render(browser, icon_html(size), size, size, 1, icons / f"mathutils-studio-{size}.png")
        print("icono", size)
    write_ico([(s, icons / f"mathutils-studio-{s}.png") for s in (16, 24, 32, 48, 64, 128, 256)],
              icons / "mathutils-studio.ico")
    (icons / "mathutils-studio.svg").write_text(icon_svg(), encoding="utf-8")

    installer = ASSETS / "installer"
    installer.mkdir(parents=True, exist_ok=True)
    for width, height in WIZARD_WIDTHS:
        scale = width / 164
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            png = Path(tmp) / "wizard.png"
            render(browser, wizard_html(164, 314, version), 164, 314, scale, png)
            flatten_to_bmp(png, installer / f"wizard-{width}.bmp")
        print("imagen del asistente", width)
    for side in SMALL_SIDES:
        with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as tmp:
            png = Path(tmp) / "small.png"
            render(browser, small_html(55), 55, 55, side / 55, png)
            flatten_to_bmp(png, installer / f"small-{side}.bmp")
        print("imagen pequeña", side)
    shutil.copy(icons / "mathutils-studio.ico", installer / "uninstall.ico")
    # Vista previa del asistente para revisar el resultado sin instalar nada
    shutil.copy(installer / "wizard-328.bmp", Path(tempfile.gettempdir()) / "mathutils-wizard-preview.bmp")


if __name__ == "__main__":
    main()
