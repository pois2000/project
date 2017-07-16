<?php
$hostTel="01012341234";
$eventID = date("mdyhis").substr($hostTel,7,11);  //eventID  생성 규칙 YYMMDDHHMMSSXXXX 년월일시분초+전화번호뒷네자리
print($eventID);


INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0002.png', 'mole0002.mp3', '0708170250251547', '사랑해!')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0003.png', 'mole0003.mp3', '0708170250251547', '메시지')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0004.png', 'mole0004.mp3', '0708170250251547', '메시지')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0005.png', 'mole0005.mp3', '0708170250251547', '메시지')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0006.png', 'mole0006.mp3', '0708170250251547', '메시지')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0007.png', 'mole0007.mp3', '0708170250251547', '메시지')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0008.png', 'mole0008.mp3', '0708170250251547', '메시지')

INSERT INTO `tbl_users` (`userName`, `userTel`, `userPic`, `userSound`, `eventID`, `message`) VALUES ('ㅇㅇㅇ', '', 'mole0009.png', 'mole0009.mp3', '0708170250251547', '메시지')
