<?php
$hostTel="01012341234";
$eventID = date("mdyhis").substr($hostTel,7,11);  //eventID  생성 규칙 YYMMDDHHMMSSXXXX 년월일시분초+전화번호뒷네자리
print($eventID);

 ?>
