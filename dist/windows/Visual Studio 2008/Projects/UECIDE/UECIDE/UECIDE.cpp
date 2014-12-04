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
#include <iostream>
#include <sstream>

#define BUFSIZE 4096
#define VARNAME TEXT("JAVA_HOME")

using namespace std;

void runJar(LPCWSTR jar) {

	wstring jarw(jar);
	wstring param = L"-jar " + jarw;
#if 0
#define NEXE 5
	const wchar_t *exetry[NEXE] = {
		L"C:\\Program Files\\Java\\jre1.8.0_27\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_27\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_26\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_26\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_25\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_25\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_24\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_24\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_23\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_23\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_22\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_22\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_21\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_21\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre1.8.0_20\\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre1.8.0_20\\bin\\javaw.exe",
		L"C:\\Program Files\\Java\\jre7\bin\\javaw.exe",
		L"C:\\Program Files (x86)\\Java\\jre7\\bin\\javaw.exe",
		L"java\\bin\\javaw.exe"
	};
#endif
	wstring exe = L"";

    // Try finding a javaw.exe file.
    // First, look for JRE 8:

	for (unsigned short i = 20; i < 99; i++) {
		wstringstream pathstr;
		wstring path;
		wstring path_prefix_64 = L"C:\\Program Files\\Java\\jre1.8.0_";
        wstring path_prefix_32 = L"C:\\Program Files (x86)\\Java\\jre1.8.0_";
		wstring path_suffix = L"\\bin\\javaw.exe";

        pathstr << path_prefix_64;
        pathstr << i;
        pathstr << path_suffix;

		path = pathstr.str();

		OutputDebugString(path.c_str());
		OutputDebugString(L"\r\n");
 
		if (PathFileExists(path.c_str())) {
			exe = path;
			break;
		}

		pathstr.str(L"");
        pathstr << path_prefix_32;
        pathstr << i;
        pathstr << path_suffix;

		path = pathstr.str();

		OutputDebugString(path.c_str());
		OutputDebugString(L"\r\n");

		if (PathFileExists(path.c_str())) {
			exe = path;
			break;
		}
	}

	// Not 8? Ok, look for 7.
	if (exe == L"") {
		wstring path = L"C:\\Program Files\\Java\\jre7\bin\\javaw.exe";
		if (PathFileExists(path.c_str())) {
			exe = path;
		} else {
			path = L"C:\\Program Files (x86)\\Java\\jre7\bin\\javaw.exe";
			if (PathFileExists(path.c_str())) {
				exe = path;
			}
		}
	}

	// Not 7? Ok, look for local.
	if (exe == L"") {
		wstring path = L"java\\bin\\javaw.exe";
		if (PathFileExists(path.c_str())) {
			exe = path;
		}
	}

	// Not even there? Urk!
	if (exe == L"") {
		OutputDebugString(L"Error: unable to locate java!\r\n");
		return;
	}

	int argCount;
	wchar_t **argList = CommandLineToArgvW(GetCommandLineW(), &argCount);
	for (int i = 1; i < argCount; i++) {
		wstring warg(argList[i]);
		param += L" " + warg;
	}

	const wchar_t *paramptr = param.c_str();
    const wchar_t *exec = exe.c_str();

	ShellExecute(
		NULL,
		L"open",
		exec,
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



