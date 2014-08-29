    !define VERSION "%VERSION%"
    !define PRODUCT "%PRODUCT%"
    !define PRODUCTCAP "%PRODUCTCAP%"
    !define PUBLISHER "%PUBLISHER%"
    !define TYPE "%TYPE%"

    !define MUI_FILE "savefile"
    !define MUI_BRANDINGTEXT "${PRODUCTCAP} ${VERSION}"
    CRCCheck On

    !include "${NSISDIR}\Contrib\Modern UI\System.nsh"

    ShowInstDetails "nevershow" 
    ShowUninstDetails "nevershow"

    !define MUI_ICON "../../dist/windows/UECIDE.ico"
    !define MUI_UNICON "../../dist/windows/UECIDE.ico"

    InstallDir "$PROGRAMFILES\${PUBLISHER}\${PRODUCTCAP}"

    !insertmacro MUI_LANGUAGE "English"

    LicenseData "../../dist/windows/readme.txt"
    LicenseForceSelection radiobuttons "I accept" "I decline"

    Name "${PRODUCTCAP} ${VERSION}"
    OutFile "${PRODUCT}-${VERSION}-${TYPE}.exe"

    RequestExecutionLevel admin

    page license
    page directory
    page instfiles
    uninstPage uninstConfirm 
    uninstPage instFiles

Section "Install" 

    SetShellVarContext all
    SetOutPath "$INSTDIR"
    File /r "uecide-${VERSION}/*"

    CreateShortCut "$DESKTOP\${PRODUCTCAP}.lnk" "$INSTDIR\${PRODUCT}.exe" "" "$INSTDIR\${PRODUCT}.exe" 0
 
    CreateDirectory "$SMPROGRAMS\${PUBLISHER}"
    CreateDirectory "$SMPROGRAMS\${PUBLISHER}\${PRODUCTCAP}"
    CreateShortCut "$SMPROGRAMS\${PUBLISHER}\${PRODUCTCAP}\Uninstall.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
    CreateShortCut "$SMPROGRAMS\${PUBLISHER}\${PRODUCTCAP}\${PRODUCTCAP}.lnk" "$INSTDIR\${PRODUCT}.exe" "" "$INSTDIR\${PRODUCT}.exe" 0
 
;write uninstall information to the registry
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTCAP}" "DisplayName" "${PRODUCTCAP} (remove only)"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTCAP}" "UninstallString" "$INSTDIR\Uninstall.exe"
 
    WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

Section "Uninstall"
 
    SetShellVarContext all
;Delete Files 
  RMDir /r "$INSTDIR\*.*"    
 
;Remove the installation directory
  RMDir "$INSTDIR"
 
;Delete Start Menu Shortcuts
  Delete "$DESKTOP\${PRODUCTCAP}.lnk"
  RMDir /r "$SMPROGRAMS\${PUBLISHER}\${PRODUCTCAP}\*.*"
  RMDir "$SMPROGRAMS\${PUBLISHER}\${PRODUCTCAP}"
  RMDir "$SMPROGRAMS\${PUBLISHER}"
 
;Delete Uninstaller And Unistall Registry Entries
  DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\${PRODUCTCAP}"
  DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCTCAP}"  
 
SectionEnd
 
 
;--------------------------------    
;MessageBox Section
