<?php

	require_once 'dbconfig.php';

	if(isset($_GET['delete_id']))
	{
		// select image from db to delete
		$stmt_select = $DB_con->prepare('SELECT userPic, calleeTel, userSound FROM tbl_users WHERE userID =:uid');
		$stmt_select->execute(array(':uid'=>$_GET['delete_id']));
		$delRow=$stmt_select->fetch(PDO::FETCH_ASSOC);

		unlink('user/'.$delRow['calleeTel'].'/img/'.$delRow['userPic']); //기존 파일 삭제
		unlink('user/'.$delRow['calleeTel'].'/sound/'.$delRow['userSound']); //기존 파일 삭제


		// it will delete an actual record from db
		$stmt_delete = $DB_con->prepare('DELETE FROM tbl_users WHERE userID =:uid');
		$stmt_delete->bindParam(':uid',$_GET['delete_id']);
		$stmt_delete->execute();

		header("Location: view2.php");
	}

?>
<html>
<head>
<title>Upload, Insert, Update, Delete an Image using PHP MySQL - Coding Cage</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.0/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
</head>

<body>

<div class="container">

	<div class="page-header">
    	<h1 class="h2">누구나 / <a class="btn btn-default" href="addnew.php">
				<span class="glyphicon glyphicon-plus"></span> &nbsp; 사진추가 </a></h1>
    </div>

<br />

<div class="row">
<?php

	$stmt = $DB_con->prepare('SELECT userID, calleeName, calleeTel, userName, userTel, userPic, userSound FROM tbl_users ORDER BY userID DESC');
	$stmt->execute();

	if($stmt->rowCount() > 0)
	{
		while($row=$stmt->fetch(PDO::FETCH_ASSOC))
		{
			extract($row);
			$temp = explode('.', $row['userSound']); // 사운드 확장자 찾아내기
			?>
			<div class="col-xs-3">
				<p class="page-header"><?php echo $userName."&nbsp;/&nbsp;".$userTel; ?></p>
				<img src="../user/<?php echo $row['calleeTel']."/img/".$row['userPic']; ?>" class="img-rounded" width="200px" height="300px" />
				<audio controls>
				  <source src="../user/<?php echo $row['calleeTel']."/sound/".$row['userSound']; ?>"  type="audio/<?php echo $temp[1] ?>">
				  Your browser does not support the audio tag.
				</audio>
				<p class="page-header">
				<span>
				<a class="btn btn-info" href="editform.php?edit_id=<?php echo $row['userID']; ?>" title="click for edit"><span class="glyphicon glyphicon-edit"></span> 수정</a>
				<a class="btn btn-danger" href="?delete_id=<?php echo $row['userID'];?>" title="click for delete" onclick="return confirm('sure to delete ?')"><span class="glyphicon glyphicon-remove-circle"></span> 삭제</a>
				</span>
				</p>
			</div>
			<?php
		}
	}
	else
	{
		?>
        <div class="col-xs-12">
        	<div class="alert alert-warning">
            	<span class="glyphicon glyphicon-info-sign"></span> &nbsp; No Data Found ...
            </div>
        </div>
        <?php
	}

?>
</div>
</div>
</body>
</html>
