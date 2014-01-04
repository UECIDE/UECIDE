<?php
include_once("lib.php");


$plugins = array();
$cores = array();
$boards = array();
$compilers = array();

$platform = "any";
if (array_key_exists("platform", $_GET)) {
    $platform = $_GET['platform'];
}
$arch = "any";
if (array_key_exists("arch", $_GET)) {
    $arch = $_GET['arch'];
}

header("Content-type: application/json");

$platform = str_replace("%", "", $platform);
$platform = str_replace("#", "", $platform);
$platform = str_replace("\\", "", $platform);
$platform = str_replace("/", "", $platform);
$platform = str_replace(".", "", $platform);

$arch = str_replace("%", "", $arch);
$arch = str_replace("#", "", $arch);
$arch = str_replace("\\", "", $arch);
$arch = str_replace("/", "", $arch);
$arch = str_replace(".", "", $arch);

if (file_exists("/var/www/uecide/cache/$platform/$arch.json")) {
    print file_get_contents("/var/www/uecide/cache/$platform/$arch.json");
    exit;
}

$dir = opendir("/var/www/uecide/p/plugins");
while ($plugin = readdir($dir)) {
    if (substr($plugin,0,1) == ".") {
        continue;
    }
    if (!(substr($plugin, -4, 4) == ".jar")) {
        continue;
    }
    $man = manifest("/var/www/uecide/p/plugins/$plugin");
    $targetPlatform = "any";
    if (array_key_exists("Platform", $man)) {
        $targetPlatform = $man['Platform'];
    }

    if ($targetPlatform == $platform || $targetPlatform == $platform . "_" . $arch || $targetPlatform == "any") {
        $plugins[substr($plugin, 0, -4)] = array(
            "version" => $man['Version'],
            "url" => "http://uecide.org/p/plugins/$plugin",
        );
        foreach ($man as $k=>$v) {
            $plugins[substr($plugin, 0, -4)][$k] = $v;
        }
    }
}
closedir($dir);

$dir = opendir("/var/www/uecide/p/cores");
while ($core = readdir($dir)) {
    if (substr($core,0,1) == ".") {
        continue;
    }
    if (!(substr($core, -4, 4) == ".jar")) {
        continue;
    }
    $man = manifest("/var/www/uecide/p/cores/$core");
    $targetPlatform = "any";
    if (array_key_exists("Platform", $man)) {
        $targetPlatform = $man['Platform'];
    }

    $corename = $man['Core'];

    if ($targetPlatform == $platform || $targetPlatform == $platform . "_" . $arch || $targetPlatform == "any") {
        $cores[$corename] = array(
            "version" => $man['Version'],
            "url" => "http://uecide.org/p/cores/$core",
        );
        foreach ($man as $k=>$v) {
            $cores[$corename][$k] = $v;
        }
    }
}
closedir($dir);

$dir = opendir("/var/www/uecide/p/boards");
while ($board = readdir($dir)) {
    if (substr($board,0,1) == ".") {
        continue;
    }
    if (!(substr($board, -4, 4) == ".jar")) {
        continue;
    }
    $man = manifest("/var/www/uecide/p/boards/$board");
    $targetPlatform = "any";
    if (array_key_exists("Platform", $man)) {
        $targetPlatform = $man['Platform'];
    }


    $boardname = $man['Board'];

    if ($targetPlatform == $platform || $targetPlatform == $platform . "_" . $arch || $targetPlatform == "any") {
        $boards[$boardname] = array(
            "version" => $man['Version'],
            "url" => "http://uecide.org/p/boards/$board",
        );
        foreach ($man as $k=>$v) {
            $boards[$boardname][$k] = $v;
        }
    }
}
closedir($dir);

$dir = opendir("/var/www/uecide/p/compilers");
while ($compiler = readdir($dir)) {
    if (substr($compiler,0,1) == ".") {
        continue;
    }
    if (!(substr($compiler, -4, 4) == ".jar")) {
        continue;
    }
    $man = manifest("/var/www/uecide/p/compilers/$compiler");
    $targetPlatform = "any";
    $targetArch = "any";
    if (array_key_exists("Platform", $man)) {
        $targetPlatform = $man['Platform'];
    }
    if (array_key_exists("Arch", $man)) {
        $targetArch = $man['Arch'];
    }

    $targetArch = split(",", $targetArch);

    $compilername = $man['Compiler'];

    if ($targetPlatform == $platform || $targetPlatform == "any") {
        if (in_array($arch, $targetArch) || in_array("any", $targetArch)) {
            $compilers[$compilername] = array(
                "version" => $man['Version'],
                "url" => "http://uecide.org/p/compilers/$compiler",
            );
            foreach ($man as $k=>$v) {
                $compilers[$compilername][$k] = $v;
            }
        }
    }
}
closedir($dir);

$versions = array(
    "plugins" => $plugins,
    "cores" => $cores,
    "boards" => $boards,
    "compilers" => $compilers
);

$data = json_encode($versions);
file_put_contents("/var/www/uecide/cache/$platform/$arch.json", $data);
print $data;
