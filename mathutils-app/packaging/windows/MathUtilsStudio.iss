; Instalador de MathUtils Studio para Windows (Inno Setup 6).
;
; Empaqueta la imagen que genera packaging/tools/build_app.py: un lanzador nativo, el runtime mínimo
; creado con jlink y la aplicación. No necesita Java en el equipo.
;
; Compilación (la hace build_app.py con --installer):
;   ISCC.exe /DAppVersion=0.6.0 MathUtilsStudio.iss

#ifndef AppVersion
  #define AppVersion "0.6.0"
#endif
#define AppName "MathUtils Studio"
#define AppExe "MathUtilsStudio.exe"
#define AppPublisher "barnesinside"
#define AppUrl "https://github.com/blacknoize404/MathUtils"
#define Dist "..\build\dist\MathUtilsStudio"
#define Assets "..\assets"

[Setup]
; Identificador estable: permite actualizar sobre una instalación previa
AppId={{6F3C1E52-2B7A-4D0E-9C41-5A8E1D7B0F36}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppUrl}
AppSupportURL={#AppUrl}/issues
AppUpdatesURL={#AppUrl}/releases
VersionInfoVersion={#AppVersion}
VersionInfoDescription={#AppName}
DefaultDirName={autopf}\{#AppName}
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
UninstallDisplayIcon={app}\{#AppExe}
UninstallDisplayName={#AppName}
SetupIconFile={#Assets}\icons\mathutils-studio.ico
; Imágenes renderizadas desde los SVG de la marca, una por cada escala de pantalla
WizardImageFile={#Assets}\installer\wizard-164.bmp,{#Assets}\installer\wizard-192.bmp,{#Assets}\installer\wizard-246.bmp,{#Assets}\installer\wizard-328.bmp
WizardSmallImageFile={#Assets}\installer\small-55.bmp,{#Assets}\installer\small-64.bmp,{#Assets}\installer\small-80.bmp,{#Assets}\installer\small-96.bmp,{#Assets}\installer\small-110.bmp
WizardStyle=modern
WizardImageStretch=no
OutputDir=..\build\dist
OutputBaseFilename=MathUtilsStudio-{#AppVersion}-windows-x64-setup
Compression=lzma2/ultra64
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; Se instala sin permisos de administrador si la persona lo prefiere
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
CloseApplications=yes
RestartApplications=no

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[CustomMessages]
spanish.Tagline=Calcula, mira el árbol y entiende cada paso.
english.Tagline=Calculate, see the tree and understand every step.
spanish.LaunchAfter=Abrir {#AppName}
english.LaunchAfter=Open {#AppName}

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#Dist}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExe}"; Comment: "{cm:Tagline}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon; Comment: "{cm:Tagline}"

[Run]
Filename: "{app}\{#AppExe}"; Description: "{cm:LaunchAfter}"; Flags: nowait postinstall skipifsilent
