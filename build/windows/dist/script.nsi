    !define VERSION "%VERSION%"

    !define MUI_FILE "savefile"
    !define MUI_BRANDINGTEXT "UECIDE ${VERSION}"
    CRCCheck On

    !include "${NSISDIR}\Contrib\Modern UI\System.nsh"

    ShowInstDetails "nevershow" 
    ShowUninstDetails "nevershow"

    !define MUI_ICON "../themes/uecide/windows/application.ico"
    !define MUI_UNICON "../themes/uecide/windows/application.ico"

    InstallDir "$PROGRAMFILES\Majenko Technologies\UECIDE"

    !insertmacro MUI_LANGUAGE "English"

    LicenseData "dist/readme.txt"
    LicenseForceSelection radiobuttons "I accept" "I decline"

    Name "UECIDE ${VERSION}"
    OutFile "uecide-${VERSION}.exe"

    RequestExecutionLevel admin

    page license
    page directory
    page instfiles
    uninstPage uninstConfirm 
    uninstPage instFiles

Section "Install" 

    SetShellVarContext all
    SetOutPath "$INSTDIR"
    File /r "work/*"

    CreateShortCut "$DESKTOP\UECIDE.lnk" "$INSTDIR\uecide.exe" ""
 
    CreateDirectory "$SMPROGRAMS\Majenko Technologies"
    CreateDirectory "$SMPROGRAMS\Majenko Technologies\UECIDE"
    CreateShortCut "$SMPROGRAMS\Majenko Technologies\UECIDE\Uninstall.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
    CreateShortCut "$SMPROGRAMS\Majenko Technologies\UECIDE\UECIDE.lnk" "$INSTDIR\uecide.exe" "" "$INSTDIR\uecide.exe" 0
 
;write uninstall information to the registry
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\UECIDE" "DisplayName" "UECIDE (remove only)"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\UECIDE" "UninstallString" "$INSTDIR\Uninstall.exe"
 
    WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

Section "Uninstall"
 
    SetShellVarContext all
;Delete Files 
  RMDir /r "$INSTDIR\*.*"    
 
;Remove the installation directory
  RMDir "$INSTDIR"
 
;Delete Start Menu Shortcuts
  Delete "$DESKTOP\UECIDE.lnk"
  RMDir /r "$SMPROGRAMS\Majenko Technologies\UECIDE\*.*"
  RMDir "$SMPROGRAMS\Majenko Technologies\UECIDE"
  RMDir "$SMPROGRAMS\Majenko Technologies"
 
;Delete Uninstaller And Unistall Registry Entries
  DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\UECIDE"
  DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\UECIDE"  
 
SectionEnd
 
 
;--------------------------------    
;MessageBox Section
