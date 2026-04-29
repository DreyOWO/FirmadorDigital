# -*- coding: utf-8 -*-

# Compilar este script con NSIS de 64 bits, instalar nsis-*-negrutiu-amd64.exe desde:
# https://github.com/negrutiu/nsis/releases/latest

# Descargar Temurin JRE en .zip, extraer carpeta "jdk-*-jre" y renombrar como "jre":
# https://adoptium.net/es/temurin/releases/?arch=x64&os=windows&package=jre&version=latest

# Descargar https://firmador.libre.cr/firmador.jar
# "firmador.jar" y "jre" deben estar en la misma carpeta que este script al compilar.

# Para instalar silenciosamente el firmador, pasar el parametro /S (ese mayuscula).

Target amd64-unicode
Name Firmador

Caption "Instalador de $(^name)"
BrandingText "Instalador de $(^name)"
!define MUI_WELCOMEPAGE_TITLE "Instalador de $(^name)"
!define MUI_WELCOMEPAGE_TEXT "Presione Instalar para iniciar el proceso."

!include MUI2.nsh

SetCompressor /SOLID lzma
SetCompressorDictSize 228

!define MUI_ICON firmador.ico
!define MUI_UNICON firmador.ico
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE SpanishInternational

InstallDir $LOCALAPPDATA\Programs\$(^name)

Section
	SetOutPath -
	File /r jre
	File firmador.jar
	File firmador.ico
	WriteRegStr HKCU Software\Classes\firmador "" "URL:$(^name)"
	WriteRegStr HKCU Software\Classes\firmador "URL Protocol" ""
	WriteRegStr HKCU Software\Classes\firmador\Application "ApplicationName" "$(^name)"
	WriteRegStr HKCU Software\Classes\firmador\shell\open\command "" 'cmd.exe /q /v:on /c set PreURL="%1" & set URL=!PreURL:^&=! & start /b $INSTDIR\jre\bin\javaw.exe --add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED -Djnlp.remoteOrigin=!URL:firmador:=! -jar $INSTDIR\firmador.jar run'
	WriteRegStr HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) DisplayIcon $INSTDIR\firmador.ico
	WriteRegStr HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) DisplayName $(^name)
	WriteRegStr HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) DisplayVersion 2.0.0
	WriteRegStr HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) Publisher "Autores de Firmador"
	WriteRegStr HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) UninstallString "$INSTDIR\Desinstalar $(^name).exe /S"
	WriteRegDWORD HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) EstimatedSize 233469
	WriteRegDWORD HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) NoModify 1
	WriteRegDWORD HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name) NoRepair 1
	CreateShortcut /NoWorkingDir $SMPROGRAMS\$(^name).lnk $INSTDIR\jre\bin\javaw.exe '--add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED -jar "$INSTDIR\firmador.jar" run' $INSTDIR\firmador.ico "" "" "" "Firmar documentos digitalmente"
	CreateShortcut /NoWorkingDir $DESKTOP\$(^name).lnk $INSTDIR\jre\bin\javaw.exe '--add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED -jar "$INSTDIR\firmador.jar" run' $INSTDIR\firmador.ico "" "" "" "Firmar documentos digitalmente"
	CreateShortcut /NoWorkingDir "$SMSTARTUP\$(^name) (inicio automático).lnk" $INSTDIR\jre\bin\javaw.exe '--add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED -jar "$INSTDIR\firmador.jar" --background run' $INSTDIR\firmador.ico "" "" "" "Inicia Firmador en segundo plano para facilitar la comunicación desde sitios web que soliciten firma o autenticación"
	WriteUninstaller "$INSTDIR\Desinstalar $(^name).exe"
SectionEnd

Section Uninstall
	RMDir /r $INSTDIR\jre
	Delete $DESKTOP\$(^name).lnk
	Delete $SMPROGRAMS\$(^name).lnk
	Delete "$SMSTARTUP\$(^name) (inicio automático).lnk"
	Delete $INSTDIR\firmador.ico
	Delete $INSTDIR\firmador.jar
	DeleteRegKey HKCU Software\Classes\firmador
	DeleteRegKey HKCU Software\Microsoft\Windows\CurrentVersion\Uninstall\$(^name)
	Delete "$INSTDIR\Desinstalar $(^name).exe"
	RMDir $INSTDIR
SectionEnd

RequestExecutionLevel user
ManifestDPIAware true

VIAddVersionKey /LANG=5130 ProductName "Firmador"
VIAddVersionKey /LANG=5130 ProductVersion "2.0.0"
VIAddVersionKey /LANG=5130 FileVersion "2.0.0"
VIAddVersionKey /LANG=5130 FileDescription "Instalador de Firmador"
VIAddVersionKey /LANG=5130 LegalCopyright "Autores de Firmador"
VIProductVersion 2.0.0.0
