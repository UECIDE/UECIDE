<?php

$db = new PDO("mysql:dbname=uecide;host=localhost","username","password");

function doDownload($file) {
    global $db;
    $q = $db->prepare("SELECT * FROM download_counter WHERE filename=:f");
    $q->execute(array("f"=>$file));
    if ($r = $q->fetchObject()) {
        $q1 = $db->prepare("UPDATE download_counter SET counter=:c WHERE filename=:f");
        $q1->execute(array("c" => $r->counter+1, "f"=>$file));
    } else {
        $q1 = $db->prepare("INSERT INTO download_counter SET counter=:c, filename=:f");
        $q1->execute(array("c" => 1, "f"=>$file));
    }
    header("Location: $file");
}

function download($dir, $download, $div)
{
    print "<tr><td><a href='download.php?file=$dir/$download'>$download</a></td>";
    if (preg_match("/.jar$/", $download, $m)) {
        $man = manifest("$dir/$download");
        print "<td>" . $man['Version'] . "</td>";
    }
    $stat = stat("$dir/$download");
    switch ($div) {
        case 0:
            print "<td width='150px'>" . number_format($stat['size'],0) . " Bytes</td></tr>";
            break;
        case 1:
            print "<td width='150px'>" . number_format($stat['size']/1024,1) . " KBytes</td></tr>";
            break;
        case 2:
            print "<td width='150px'>" . number_format($stat['size']/1024/1024,1) . " MBytes</td></tr>";
            break;
    }
}

function manifest($jar)
{
    $data = `unzip -q -c '$jar' META-INF/MANIFEST.MF`;
    $data = split("\n", $data);

    $out = array();

    foreach ($data as $d) {
        if (preg_match("/^([^:]+):\s+(.*)$/", $d, $m)) {
            $out[$m[1]] = trim($m[2]);
        }
    }

    return $out;
}

function getSpamLogDataHr() {
    $data = array();
    global $db;
    $q = $db->prepare("select date_format(from_unixtime(log_time), '%Y-%m-%d %H') as hr, count(log_id) as blocks from phpbb_spam_log group by hr");
    $q->execute();   
    while ($r = $q->fetchObject()) {
        $data[$r->hr] = $r->blocks;
    }
    return $data;
}

function getSpamLogDataDy() {
    $data = array();
    global $db;
    $q = $db->prepare("select date_format(from_unixtime(log_time), '%Y-%m-%d') as hr, count(log_id) as blocks from phpbb_spam_log group by hr");
    $q->execute();   
    while ($r = $q->fetchObject()) {
        $data[$r->hr] = $r->blocks;
    }
    return $data;
}

function getSpammers() {
    $data = array();
    global $db;
    $q = $db->prepare("select distinct log_ip from phpbb_spam_log");
    $q->execute();
    while ($r = $q->fetchObject()) {
        $data[] = $r->log_ip;
    }
    return $data;
}
