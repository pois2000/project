<?php

	error_reporting( ~E_NOTICE );

	require_once 'dbconfig.php';

	include("resize-class.php");
	include("find_file_end_num.php");
	include("thumbnail.php");

	if(isset($_GET['edit_id']) && !empty($_GET['edit_id']))
	{
		$id = $_GET['edit_id'];
		$stmt_edit = $DB_con->prepare('SELECT userID, calleeName, calleeTel, userName, userTel, userPic, userSound FROM tbl_users WHERE userID =:uid');
		$stmt_edit->execute(array(':uid'=>$id));
		$edit_row = $stmt_edit->fetch(PDO::FETCH_ASSOC);
		extract($edit_row);

		$temp = explode('.', $edit_row['userPic']);
		$imgOld = $temp[0].".";
		$temp = explode('.', $edit_row['userSound']);
		$sndOld = $temp[0].".";

	}
	else
	{
		header("Location: index.php");
	}

	if(isset($_POST['btn_save_updates']))
	{
		$userName = $_POST['user_name'];// user name
		$userTel = $_POST['user_tel'];// user tel

		$calleeName = $_POST['callee_name'];// callee name
		$calleeTel = $_POST['callee_tel'];// callee tel
		$imgFile = $_FILES['user_pic']['name'];

		$tmp_dir = $_FILES['user_pic']['tmp_name'];
		$imgSize = $_FILES['user_pic']['size'];
		$sndFile = $_FILES['user_sound']['name'];
		$tmp_dir2 = $_FILES['user_sound']['tmp_name'];


		if($imgFile)
		{
			$upload_dir = 'user/'.$calleeTel; // upload directory

			$imgExt = strtolower(pathinfo($imgFile,PATHINFO_EXTENSION)); // get image extension

			$sndExt = strtolower(pathinfo($sndFile,PATHINFO_EXTENSION)); // get sound extension

			$valid_extensions = array('jpeg', 'jpg', 'png', 'gif'); // valid extensions
			$userPic = $imgOld.$imgExt;
			if(!$sndExt){
				$userSound = $sndOld.$sndExt;
			}
			else {
				$userSound = "";
			}

			if(in_array($imgExt, $valid_extensions))
			{
				if($imgSize < 5000000)
				{
					unlink($upload_dir.'/img/'.$edit_row['userPic']); //기존 파일 삭제
					move_uploaded_file($tmp_dir,$upload_dir.'/img/'.$userPic);

					if(!$userSound){
						unlink($upload_dir.'/sound/'.$edit_row['userSound']); //기존 파일 삭제
					}
					move_uploaded_file($tmp_dir2,$upload_dir.'/sound/'.$userSound);

					$filepath = $upload_dir."/img/".$userPic;
					$new_width = 200;
					$new_height = 300;

					if($imgExt=="png"){
					 pngresize($filepath,$new_width,$new_height);
					}
					else{
						// *** 1) Initialise / load image
						$resizeObj = new resize($filepath);
						// *** 2) Resize image (options: exact, portrait, landscape, auto, crop)
						$resizeObj -> resizeImage($new_width, $new_height, 'crop');
						// *** 3) Save image
						$resizeObj -> saveImage($filepath, 1000);
					}
				}
				else
				{
					$errMSG = "5MB 이하 사진을 선택해주세요.";
				}
			}
			else
			{
				$errMSG = "JPG, JPEG, PNG & GIF 만 가능합니다.";
			}
		}
		else
		{
			// if no image selected the old image remain as it is.
			$userPic = $edit_row['userPic']; // old image from database
		}


		// if no error occured, continue ....
		if(!isset($errMSG))
		{
			$stmt = $DB_con->prepare('UPDATE tbl_users
												     SET userName=:uname,userTel=:utel, userPic=:upic, userSound=:usound, calleeName=:rname, calleeTel=:rtel
											       WHERE userID=:uid');
			$stmt->bindParam(':uname',$userName);
			$stmt->bindParam(':utel',$userTel);
			$stmt->bindParam(':upic',$userPic);
			$stmt->bindParam(':usound',$userSound);
			$stmt->bindParam(':rname',$calleeName);
			$stmt->bindParam(':rtel',$calleeTel);
			$stmt->bindParam(':uid',$id);

			if($stmt->execute()){
				?>
        <script>
				alert('Successfully Updated ...');
				window.location.href='index.php';
				</script>
                <?php
			}
			else{
				$errMSG = "Sorry Data Could Not Updated !";
			}
		}
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
    	<h1 class="h2">수정 <a class="btn btn-default" href="index.php"> 모든 멤버 </a></h1>
    </div>

<div class="clearfix"></div>

<form method="post" enctype="multipart/form-data">


    <?php
	if(isset($errMSG)){
		?>
        <div class="alert alert-danger">
          <span class="glyphicon glyphicon-info-sign"></span> &nbsp; <?php echo $errMSG; ?>
        </div>
        <?php
	}
	?>

	  <div class="form-group">
			<label class="control-label">내 이름</label>

      <input class="form-control" type="text" name="user_name" value="<?php echo $userName; ?>" required />
		</div>

		  <div class="form-group">
				<label class="control-label">내 번호(선택)</label>
				<input class="form-control" type="text" name="user_tel" value="<?php echo $userTel; ?>" />
			</div>
			<div class="form-group">
				<label class="control-label">친구 이름(선택)</label>
				<input class="form-control" type="text" name="callee_name" value="<?php echo $calleeName; ?>"/>
		  </div>

			<div class="form-group">
				<label class="control-label">친구 번호</label>
				<input class="form-control" type="text" name="callee_tel"  value="<?php echo $calleeTel; ?>" required/>
		  </div>


			  <div class="form-group">
					<label class="control-label">축하 사진</label>

        	<p><img src="user/<?php echo $calleeTel."/img/".$userPic; ?>" height="300" width="200" /></p>
        	<input class="input-group" type="file" name="user_pic" accept="image/*" />
				</div>

			  <div class="form-group">
					<label class="control-label">축하 음성(선택)</label>
					<audio controls>
					  <source src="user/<?php echo $calleeTel."/sound/".$userSound; ?>"  type="audio/*">
					  Your browser does not support the audio tag.
					</audio>
        	<input class="input-group" type="file" name="user_sound" accept="audio/*" />
				</div>

			  <div class="form-group">

        <button type="submit" name="btn_save_updates" class="btn btn-default">
        <span class="glyphicon glyphicon-save"></span> 적용
        </button>

        <a class="btn btn-default" href="index.php"> <span class="glyphicon glyphicon-backward"></span> 취소 </a>
			</div>
    </table>
</form>

</div>
</body>
</html>
