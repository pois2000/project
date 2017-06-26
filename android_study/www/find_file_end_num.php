<?
function getFileNameList($dir){
  $arr_filename = Array();
  if ($handle = opendir($dir)) { // 파일검색 디렉토리 지정
    while ('' != ($file = readdir($handle))) { // 파일명을 넘겨받음
      if ($file != '..'&&$file != '.' ) { // 파일명이 ..이 아닌 것만
        // $content = file_get_contents($file); // 파일내용 읽어오기
        // if ('' != strstr($content, $str)) { // 문자열 'mole'이 포함된..
          array_push($arr_filename, $file); //
          // echo "file : $file <br />"; // 파일명 출력
        // }
      }
    }
    closedir($handle); // 디렉토리 핸들 해제
  }
  sort($arr_filename);
  // print_r($arr_filename);
  // $lastcount = count($arr_filename);
  $lastfilename = end($arr_filename);
  $num = preg_replace("/[^0-9]*/s", "", $lastfilename);
  // echo "last file name: ".$lastfilename."<br />last count:".$lastcount."<br />file name is num?".is_numeric($num);
  return $num; //마지막 숫자 리턴
}

?>
