<?

// [$dir경로의 파일중 $str문자열을 파일내용에 포함하고 있는 파일찾기]
// @param [string] $dir [경로]
// @param [string] $str [문자열]
// @return [array] [$str문자열을 포함하고 있는 파일]

function getFileNameList($dir){
  // $str = "mole";
  $arr_filename = Array();
  if ($handle = opendir($dir)) { // 파일검색 디렉토리 지정
    while ('' != ($file = readdir($handle))) { // 파일명을 넘겨받음
      if ($file != '..') { // 파일명이 ..이 아닌 것만
        // $content = file_get_contents($file); // 파일내용 읽어오기
        // if ('' != strstr($content, $str)) { // 문자열 'mole'이 포함된..
          array_push($arr_filename, $file); //
          // echo "$file"; // 파일명 출력
        // }
      }
    }
    closedir($handle); // 디렉토리 핸들 해제
  }
  return count($arr_filename)-1; //개수 리턴
}

?>
