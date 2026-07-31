"""Construye MathUtils Studio como aplicación nativa e independiente de Java.

Pasos (los mismos en Windows y en Linux):

1. ``mvn package``: compila los módulos ``MathUtils`` (librería) y ``barnesinside.mathutils`` (app).
2. Descarga los jmods de JavaFX de la plataforma (una vez; quedan en ``packaging/build``).
3. ``jlink`` crea un runtime con solo los módulos que la app necesita realmente: se parte de
   ``barnesinside.mathutils`` y el enlazador añade lo que sus ``requires`` arrastran, nada más.
4. ``jpackage --type app-image`` envuelve ese runtime con un lanzador nativo (``MathUtilsStudio.exe`` en
   Windows, ``bin/MathUtilsStudio`` en Linux) y el icono de la marca.

El resultado queda en ``packaging/build/dist/MathUtilsStudio``. No hace falta Java en la máquina
que lo ejecute. En Windows, ``packaging/windows/MathUtilsStudio.iss`` lo empaqueta como instalador.

Uso::

    python packaging/tools/build_app.py [--deb] [--skip-maven]
"""
import argparse
import platform
import re
import shutil
import subprocess
import sys
import tarfile
import urllib.request
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
PACKAGING = HERE.parent
APP = PACKAGING.parent
ROOT = APP.parent
BUILD = PACKAGING / "build"
ASSETS = PACKAGING / "assets"

JAVAFX_VERSION = "21.0.5"
MODULE = "barnesinside.mathutils"
MAIN_CLASS = "barnesinside.mathutils.Launcher"
NAME = "MathUtilsStudio"

IS_WINDOWS = platform.system() == "Windows"


def run(command: list, **kwargs) -> None:
    print("$", " ".join(str(c) for c in command))
    subprocess.run([str(c) for c in command], check=True, **kwargs)


def remove_tree(folder: Path) -> None:
    """Borra una carpeta aunque jlink haya dejado sus archivos de solo lectura (ocurre en Windows)."""
    def make_writable(function, path, _error):
        Path(path).chmod(0o700)
        function(path)
    shutil.rmtree(folder, onexc=make_writable) if sys.version_info >= (3, 12) else shutil.rmtree(folder, onerror=make_writable)


def project_version() -> str:
    match = re.search(r"<version>([\d.]+)", (ROOT / "pom.xml").read_text(encoding="utf-8"))
    parts = (match.group(1) if match else "1.0").split(".")
    while len(parts) < 3:
        parts.append("0")
    return ".".join(parts[:3])


def java_tool(name: str) -> str:
    home = shutil.which("jpackage")
    if home:
        return str(Path(home).with_name(name + (".exe" if IS_WINDOWS else "")))
    sys.exit("No encuentro jpackage: usa un JDK 21 o superior en el PATH.")


def main_jar(folder: Path, prefix: str) -> Path:
    """El jar principal de un módulo, sin los de fuentes ni documentación."""
    jars = [j for j in folder.glob(f"{prefix}-*.jar") if not j.stem.endswith(("-sources", "-javadoc", "-tests"))]
    if not jars:
        sys.exit(f"No hay jar de {prefix} en {folder}: compila con Maven primero.")
    return jars[0]


def javafx_platform() -> str:
    machine = platform.machine().lower()
    arch = "aarch64" if machine in ("arm64", "aarch64") else "x64"
    return ("windows" if IS_WINDOWS else "osx" if platform.system() == "Darwin" else "linux") + "-" + arch


def fetch_javafx_jmods() -> Path:
    target = BUILD / f"javafx-jmods-{JAVAFX_VERSION}"
    if target.exists():
        return target
    BUILD.mkdir(parents=True, exist_ok=True)
    url = (f"https://download2.gluonhq.com/openjfx/{JAVAFX_VERSION}/"
           f"openjfx-{JAVAFX_VERSION}_{javafx_platform()}_bin-jmods.zip")
    archive = BUILD / "javafx-jmods.zip"
    print("Descargando", url)
    urllib.request.urlretrieve(url, archive)
    with zipfile.ZipFile(archive) as z:
        z.extractall(BUILD / "javafx-extract")
    extracted = next((BUILD / "javafx-extract").glob("javafx-jmods-*"))
    shutil.move(str(extracted), target)
    shutil.rmtree(BUILD / "javafx-extract", ignore_errors=True)
    archive.unlink()
    return target


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--deb", action="store_true", help="en Linux, genera también el paquete .deb")
    parser.add_argument("--installer", action="store_true", help="en Windows, compila también el instalador Inno Setup")
    parser.add_argument("--skip-maven", action="store_true", help="reutiliza los jar ya compilados")
    args = parser.parse_args()

    if not args.skip_maven:
        run(["mvn", "-B", "-DskipTests", "-pl", "mathutils-app", "-am", "package"], cwd=ROOT, shell=IS_WINDOWS)

    app_jar = main_jar(APP / "target", "mathutils-app")
    core_jar = main_jar(ROOT / "mathutils-core" / "target", "mathutils-core")
    fx = fetch_javafx_jmods()
    jdk_jmods = Path(java_tool("jlink")).parent.parent / "jmods"
    if not jdk_jmods.exists():
        sys.exit(f"El JDK no trae jmods ({jdk_jmods}). Usa una distribución con jmods, como Temurin.")

    runtime = BUILD / "runtime"
    dist = BUILD / "dist"
    for folder in (runtime, dist):
        if folder.exists():
            remove_tree(folder)

    modules = [str(p) for p in (jdk_jmods, fx, app_jar, core_jar)]
    run([java_tool("jlink"), "--module-path", ";".join(modules) if IS_WINDOWS else ":".join(modules),
         "--add-modules", MODULE, "--strip-debug", "--no-header-files", "--no-man-pages",
         "--compress", "zip-6", "--output", runtime])
    names = subprocess.run([str(Path(runtime) / "bin" / ("java.exe" if IS_WINDOWS else "java")), "--list-modules"],
                           capture_output=True, text=True, check=True).stdout.split()
    print(f"Runtime: {len(names)} módulos ->", ", ".join(m.split("@")[0] for m in names))

    icon = ASSETS / "icons" / ("mathutils-studio.ico" if IS_WINDOWS else "mathutils-studio-512.png")
    command = [java_tool("jpackage"), "--type", "app-image", "--name", NAME, "--dest", dist,
               "--module", f"{MODULE}/{MAIN_CLASS}", "--runtime-image", runtime, "--icon", icon,
               "--app-version", project_version(), "--vendor", "barnesinside",
               "--description", "MathUtils Studio: calculadora, solucionadores paso a paso y paneles matemáticos"]
    run(command)

    image = dist / NAME
    size = sum(f.stat().st_size for f in image.rglob("*") if f.is_file()) / 1e6
    print(f"\nAplicación lista en {image} ({size:.0f} MB)")

    if IS_WINDOWS and args.installer:
        build_installer()

    if not IS_WINDOWS:
        install_linux_extras(image)
        archive = dist / f"{NAME}-{project_version()}-linux-{platform.machine()}.tar.gz"
        with tarfile.open(archive, "w:gz") as tar:
            tar.add(image, arcname=NAME)
        print("Paquete", archive)
        if args.deb:
            run([java_tool("jpackage"), "--type", "deb", "--name", NAME.lower(), "--dest", dist,
                 "--module", f"{MODULE}/{MAIN_CLASS}", "--runtime-image", runtime, "--icon", icon,
                 "--app-version", project_version(), "--vendor", "barnesinside",
                 "--linux-shortcut", "--linux-menu-group", "Education;Science;Math",
                 "--description", "Calculadora y solucionadores matemáticos paso a paso"])


def find_iscc() -> str:
    found = shutil.which("iscc") or shutil.which("ISCC")
    if found:
        return found
    for base in (r"C:\Program Files (x86)\Inno Setup 6", r"C:\Program Files\Inno Setup 6",
                 str(Path.home() / "AppData" / "Local" / "Programs" / "Inno Setup 6")):
        candidate = Path(base) / "ISCC.exe"
        if candidate.exists():
            return str(candidate)
    sys.exit("No encuentro Inno Setup 6 (ISCC.exe). Instálalo desde https://jrsoftware.org/isinfo.php")


def build_installer() -> None:
    run([find_iscc(), f"/DAppVersion={project_version()}", "MathUtilsStudio.iss"], cwd=PACKAGING / "windows")
    for setup in (BUILD / "dist").glob("*-setup.exe"):
        print(f"Instalador {setup} ({setup.stat().st_size / 1e6:.0f} MB)")


def install_linux_extras(image: Path) -> None:
    """Archivos de escritorio para el tar.gz: el .desktop y el icono vectorial."""
    shutil.copy(PACKAGING / "linux" / "mathutils-studio.desktop", image / "mathutils-studio.desktop")
    shutil.copy(ASSETS / "icons" / "mathutils-studio.svg", image / "mathutils-studio.svg")
    shutil.copy(PACKAGING / "linux" / "install.sh", image / "install.sh")
    (image / "install.sh").chmod(0o755)


if __name__ == "__main__":
    main()
