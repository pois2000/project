<html>
<head>
	<meta charset="utf-8">
	<title>
		러브롤 삭제</title>
	<link rel="stylesheet" href="style.css?v=<?=time();?>">
	<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
	<link rel="icon" href="favicon.ico" type="image/x-icon">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.0/jquery.min.js"></script>
	<link href="https://fonts.googleapis.com/css?family=Lobster" rel="stylesheet">
	<link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
</head>
	<body>
		<form method="post" enctype="multipart/form-data">
					<div class="form-group">
				<label class="control-label"><i class="material-icons">fiber_pin</i>비밀번호</label>
				<input class="form-control" type="text" name="pin" placeholder="럽롤 참여 비밀번호 숫자 4자리" />
		  </div>
			<div class="form-group">
				<button type="submit" name="btnsave" class="save_btn">
		    	<i class="material-icons">done_all</i>확인
		    </button>
			</div>
		</form>

	</body>
</html>
<?php

$get_pin = $_POST['pin'];
require_once 'dbconfig.php';

	if(isset($_GET['delete_id']))
	{
		// select image from db to delete
		$stmt_select = $DB_con->prepare('SELECT userPic, eventID, userSound FROM tbl_users WHERE userID =:uid');
		$stmt_select->execute(array(':uid'=>$_GET['delete_id']));
		$delRow=$stmt_select->fetch(PDO::FETCH_ASSOC);

		$stmt_select2 = $DB_con->prepare('SELECT calleeTel FROM event_table WHERE eventID =:eID');
		$stmt_select2->execute(array(':eID'=>$delRow['eventID']));
		$delRow2=$stmt_select2->fetch(PDO::FETCH_ASSOC);

		$origin_pin = $delRow2['pin'];
	}
	if(isset($_POST['btnsave']))
	{
		if(strcmp($get_pin, $origin_pin)){
			// unlink("user/".$imgRow['userPic']);
			unlink('user/'.$delRow2['calleeTel'].'/img/'.$delRow['userPic']); //기존 파일 삭제
			print("image deleted");
			unlink('user/'.$delRow2['calleeTel'].'/sound/'.$delRow['userSound']); //기존 파일 삭제
			print("sound deleted");

			// it will delete an actual record from db
			$stmt_delete = $DB_con->prepare('DELETE FROM tbl_users WHERE userID =:uid');
			$stmt_delete->bindParam(':uid',$_GET['delete_id']);
			$stmt_delete->execute();

			print("DB 삭제 끝");

			header("Location: index.php");
		}
		else{
		header('refresh:5;index.php?id=1000010000100001'); //기본 페이지 지정
	}
}
?>
