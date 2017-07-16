<?php
require_once 'dbconfig.php';

$result = $DB_con->prepare('SELECT * FROM event_table ORDER By eventID DESC');
$result->execute();
$count = $result->rowCount();
if($count > 0)
{
	while($row=$result->fetch(PDO::FETCH_ASSOC))
	{
		extract($row);
		$eventIDt[] = $row['eventID']; //이벤트ID 리스트 저장
		$member[] = array($row['calleeName'],$row['calleeTel'],$row['hostName'], //나머지 이벤트 저장
								$row['purpose'],"pic_count","first_filename");
	}
}

$stmt = $DB_con->prepare('SELECT * FROM tbl_users ORDER By eventID DESC');
	$stmt->execute();
	$count = $stmt->rowCount();
	if($stmt->rowCount() > 0)
	{
		while($row=$stmt->fetch(PDO::FETCH_ASSOC))
		{
			extract($row);
			$eventIDs[] = $row['eventID']; //저장된 중복된 이벤트ID 리스트
			$userPics[] = $row['userPic']; //이미지 전체 저장
		}
	}
	$arr=array_count_values($eventIDs); //이벤트ID의 중복 제거

	foreach ($arr as $key => $value) {
		$key2=array_search($key, $eventIDt);  //이벤트테이블에서 행위치 찾기
		$key3=array_search($key, $eventIDs);  //유저 테이블에서 행위치 찾기
    $image = rand($key3,$key3+$value-1);  //랜덤 이미지 위치 찾기
		$member[$key2][4]=$value; //이미지 개수 넣기
		$member[$key2][5]=$userPics[$image]; //이미지 파일명 넣기

	}
	$rows=sizeof($eventIDt); //이벤트 개수 찾기
	$nums=sizeof($eventIDs); //참여자 명수 찾기
	// print_r($member);
?>


<html>

<head>
  <meta charset="utf-8">
  <title>친구에게 사랑을 전하는 러브롤</title>
  <link rel="stylesheet" href="style2.css?v=<?=time();?>">
  <link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
  <link rel="icon" href="favicon.ico" type="image/x-icon">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.0/jquery.min.js"></script>
  <link href="https://fonts.googleapis.com/css?family=Lobster" rel="stylesheet">
  <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">

</head>
<body id="body">
  <div class="container" id="background-image">
  <div id="header">
    <div id="logo_message">
      <center>
        <span lang=eng>LoveRoll</span>
      </center>
        <p>친구에게 보내는 사랑의 롤페이퍼<BR />
          <?php echo $nums?>명 LoveRoll 참여중
        </p>
      </div>

<?php
	for($i=0; $i<$rows; $i++){
		echo "<div class='list'>";
//echo "<div class='list_title'>";
		//echo $member[$i][0]."</div>";
echo "<div class='thumb'><a href=index.php?id=".$eventIDt[$i].">";
		echo "<img class='list_img' width=100% height=100% src=user/".$member[$i][1]."/img/".$member[$i][5]." /></a></div>";
		echo "<div class='list_message'><p><a href=index.php?id=".$eventIDt[$i].">".$member[$i][3]."<br />";
		echo $member[$i][2]."요청, ".$member[$i][4]."명 참여중</a></p></div></div>";
	}
?>
</div></div>
<div id=button>
  <a href="createnew.php" >
    <i class="material-icons" style="font-size:40px">add_alert</i>
    <!-- 글쓰기 -->
  </a>
</div>

</body>
</html>
