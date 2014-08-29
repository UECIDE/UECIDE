// UECIDE.cpp : Defines the entry point for the application.
//

#include "stdafx.h"
#include <string.h>
#include <stdio.h>
#include "UECIDE.h"
#include <windows.h>
#include <Shellapi.h>
#include <string>
#include "Shlwapi.h"

#define BUFSIZE 4096
#define VARNAME TEXT("JAVA_HOME")

using namespace std;

void runJar(LPCWSTR jar) {

	wstring jarw(jar);
	wstring param = L"-jar " + jarw;

#define NEXE 5
	const wchar_t *exetry[NEXE] = {
		L"C:\\Program Files\\Java\\jre1.8.0_20\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_20\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre7\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre7\\bin\\javaw.exe",
		L"java\\bin\\javaw.exe"
	};

	const wchar_t *exe = 0;

	for (int i = 0; i < NEXE; i++) {
		if (PathFileExists(exetry[i])) {
			exe = exetry[i];
			break;
		}
	}

	if (exe == 0) {
		return;
	}

	int argCount;
	wchar_t **argList = CommandLineToArgvW(GetCommandLineW(), &argCount);
	for (int i = 1; i < argCount; i++) {
		wstring warg(argList[i]);
		param += L" " + warg;
	}

	const wchar_t *paramptr = param.c_str();

	ShellExecute(
		NULL,
		L"open",
		exe,
		paramptr,
		NULL,
		SW_SHOW
	);
}

int APIENTRY _tWinMain(HINSTANCE hInstance,
                     HINSTANCE hPrevInstance,
                     LPTSTR    lpCmdLine,
                     int       nCmdShow)
{
	LPCWSTR jar = L"lib\\uecide.jar";

	runJar(jar);
	return 0;
}



