#!/bin/sh
# Instala MathUtils Studio para el usuario actual en ~/.local (sin permisos de administrador).
set -e
here="$(cd "$(dirname "$0")" && pwd)"
prefix="${PREFIX:-$HOME/.local}"
target="$prefix/opt/MathUtilsStudio"
mkdir -p "$prefix/opt" "$prefix/share/applications" "$prefix/share/icons/hicolor/scalable/apps" "$prefix/bin"
rm -rf "$target"
cp -r "$here" "$target"
rm -f "$target/install.sh" "$target/mathutils-studio.desktop" "$target/mathutils-studio.svg"
cp "$here/mathutils-studio.svg" "$prefix/share/icons/hicolor/scalable/apps/mathutils-studio.svg"
sed "s|@INSTALL_DIR@|$target|" "$here/mathutils-studio.desktop" > "$prefix/share/applications/mathutils-studio.desktop"
ln -sf "$target/bin/MathUtilsStudio" "$prefix/bin/mathutils-studio"
command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$prefix/share/applications" || true
echo "MathUtils Studio instalado en $target"
