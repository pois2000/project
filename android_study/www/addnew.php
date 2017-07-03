<?php
	$tempnum = (string)$_GET['calleeTel'];
	error_reporting( ~E_NOTICE ); // avoid notice

	require_once 'dbconfig.php';
	// *** In	clude the class
	include("resize-class.php");
	include("find_file_end_num.php");
	include("thumbnail.php");

	if(isset($_POST['btnsave']))
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

		// $DB_con->query("SELECT calleeTel FROM tbl_users WHERE $who");
		if(empty($userName)){
			$errMSG = "당신의 이름을 입력해주세요.";
		}
		else if(empty($calleeTel)){
			$errMSG = "친구의 번호를 입력해주세요.";
		}
		else if(empty($imgFile)){
			$errMSG = "이미지를 선택해주세요";
		}
		else
		{
			$upload_dir = "user/".$calleeTel; // upload directory
			mkdir("user",0755,true);
			// if(is_dir(!$upload_dir)){   //디렉토리 존재 여부 확인 후 만들기
				mkdir("user/".$calleeTel,0755,true);
				mkdir($upload_dir."/img",0755,true);
				mkdir($upload_dir."/sound",0755,true);
			// }

			$imgExt = strtolower(pathinfo($imgFile,PATHINFO_EXTENSION)); // get image extension
			$sndExt = strtolower(pathinfo($sndFile,PATHINFO_EXTENSION)); // get sound extension

			// valid image extensions
			$valid_extensions = array('jpeg', 'jpg', 'png', 'gif'); // valid extensions

			$num = getFileNameList($upload_dir."/img")+1; //현재 디렉토리의  mole파일명 끝 찾기
			$num = str_pad($num, 4, '0', STR_PAD_LEFT); //파일 번호를 0000포맷으로 변경
			// rename uploading image
			$userPic = "mole".$num.".".$imgExt;
			if($sndExt){$userSound = "mole".$num.".".$sndExt;}
			else{$userSound="";}
			// allow valid image file formats
			if(in_array($imgExt, $valid_extensions)){
				// Check file size '5MB'
				if($imgSize < 5000000){

				move_uploaded_file($tmp_dir,$upload_dir."/img/".$userPic);
				move_uploaded_file($tmp_dir2,$upload_dir."/sound/".$userSound);

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
				else{
					$errMSG = "5MB 이하 사진을 선택해주세요.";
				}
			}
			else{
				$errMSG = "JPG, JPEG, PNG & GIF 만 가능합니다.";
			}
		}


		// if no error occured, continue ....
		if(!isset($errMSG))
		{
			$stmt = $DB_con->prepare('INSERT INTO tbl_users(userName,userTel,userPic,userSound,calleeName,calleeTel) VALUES(:uname, :utel, :upic, :usound, :rname, :rtel)');
			$stmt->bindParam(':uname',$userName);
			$stmt->bindParam(':utel',$userTel);
			$stmt->bindParam(':upic',$userPic);
			$stmt->bindParam(':usound',$userSound);
			$stmt->bindParam(':rname',$calleeName);
			$stmt->bindParam(':rtel',$calleeTel);

			if($stmt->execute())
			{
				$successMSG = "new record succesfully inserted ...";
				header("refresh:1;view2.php"); // redirects image view page after 5 seconds.
			}
			else
			{
				$errMSG = "error while inserting....";
			}
		}
	}
?>

<html>
<head>
<title>LoveRoll 새메시지 작성</title>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.0/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
</head>
<body>

<div class="container">


	<div class="page-header">
    	<h1 class="h2">친구에게 LoveRoll 보내기 <a class="btn btn-default" href="index.php">
				<span class="glyphicon glyphicon-eye-open"></span> &nbsp; 친구들 메시지 보기 </a></h1>
    </div>


	<?php
	if(isset($errMSG)){
			?>
            <div class="alert alert-danger">
            	<span class="glyphicon glyphicon-info-sign"></span>
							<strong><?php echo $errMSG; ?></strong>
            </div>
            <?php
	}
	else if(isset($successMSG)){
		?>
        <div class="alert alert-success">
              <strong><span class="glyphicon glyphicon-info-sign"></span> <?php echo $successMSG; ?></strong>
        </div>
        <?php
	}
	?>
<form method="post" enctype="multipart/form-data">

  <div class="form-group">
		<label class="control-label">내 이름</label>
		<input class="form-control" type="text" name="user_name" placeholder="당신의 이름" />
  </div>

  <div class="form-group">
		<label class="control-label">내 번호(선택)</label>
		<input class="form-control" type="text" name="user_tel" placeholder="당신의 번호(선택)"  />
	</div>

	<div class="form-group">
		<label class="control-label">친구 이름(선택)</label>
		<input class="form-control" type="text" name="callee_name" placeholder="선물받을 사람 이름(선택)" />
  </div>
	<div class="form-group">
		<label class="control-label">친구 번호</label>
		<input class="form-control" type="text" name="callee_tel" value="<?php echo $tempnum;?>" placeholder="선물받을 사람 전화번호"  />
  </div>

	<div class="form-group">
		<label class="control-label">축하 사진</label>
		<input class="input-group" type="file" name="user_pic" accept="image/*" />
  </div>

	<div class="form-group">
		<label class="control-label">축하 음성(선택)</label>
		<input class="input-group" type="file" name="user_sound" accept="audio/*" />
  </div>

	<div class="form-group">
		<button type="submit" name="btnsave" class="btn btn-default">
    	<span class="glyphicon glyphicon-save"></span> &nbsp; 저장
    </button>
	</div>

</form>
</div>
</body>
</html>
