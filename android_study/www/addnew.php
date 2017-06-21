<?php

	error_reporting( ~E_NOTICE ); // avoid notice

	require_once 'dbconfig.php';
	// *** In	clude the class
	include("resize-class.php");



	if(isset($_POST['btnsave']))
	{
		$username = $_POST['user_name'];// user name
		$userMSG = $_POST['user_MSG'];// user email

		$imgFile = $_FILES['user_image']['name'];
		$tmp_dir = $_FILES['user_image']['tmp_name'];
		$imgSize = $_FILES['user_image']['size'];


		if(empty($username)){
			$errMSG = "Please Enter Username.";
		}
		else if(empty($userMSG)){
			$errMSG = "Please Enter Your MSG Work.";
		}
		// else if(empty($imgFile)){
			// $errMSG = "Please Select Image File.";
		// }
		else
		{
			$upload_dir = 'user_images/'; // upload directory

			$imgExt = strtolower(pathinfo($imgFile,PATHINFO_EXTENSION)); // get image extension

			// valid image extensions
			$valid_extensions = array('jpeg', 'jpg', 'png', 'gif'); // valid extensions

			// rename uploading image
			$userpic = "mole".rand(100,9999999).".".$imgExt;

			// allow valid image file formats
			if(in_array($imgExt, $valid_extensions)){
				// Check file size '5MB'
				if($imgSize < 5000000)				{


				move_uploaded_file($tmp_dir,$upload_dir.$userpic);

					// *** 1) Initialise / load image
				$resizeObj = new resize($upload_dir.$userpic);

				// *** 2) Resize image (options: exact, portrait, landscape, auto, crop)
				$resizeObj -> resizeImage(200, 200, 'crop');
				// *** 3) Save image
				$resizeObj -> saveImage($upload_dir.$userpic, 1000);
				}
				else{
					$errMSG = "Sorry, your file is too large.";
				}
			}
			else{
				$errMSG = "Sorry, only JPG, JPEG, PNG & GIF files are allowed.";
			}
		}


		// if no error occured, continue ....
		if(!isset($errMSG))
		{
			$stmt = $DB_con->prepare('INSERT INTO tbl_users(userName,userMSG,userPic) VALUES(:uname, :uMSG, :upic)');
			// $usernane = iconv("utf8", "euckr", $usernane);
			// $userMSG = iconv("utf8", "euckr", $userMSG);
			$stmt->bindParam(':uname',$username);
			$stmt->bindParam(':uMSG',$userMSG);
			$stmt->bindParam(':upic',$userpic);

			if($stmt->execute())
			{
				$successMSG = "new record succesfully inserted ...";
				header("refresh:5;index.php"); // redirects image view page after 5 seconds.
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
    	<h1 class="h2">add new user. <a class="btn btn-default" href="index.php"> <span class="glyphicon glyphicon-eye-open"></span> &nbsp; view all </a></h1>
    </div>


	<?php
	if(isset($errMSG)){
			?>
            <div class="alert alert-danger">
            	<span class="glyphicon glyphicon-info-sign"></span> <strong><?php echo $errMSG; ?></strong>
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
<label class="control-label">Username.</label>
<input class="form-control" type="text" name="user_name" placeholder="이름을 입력하세요" value="<?php echo $username; ?>" />
  </div>

  <div class="form-group">
<label class="control-label">Message.</label>
<input class="form-control" type="text" name="user_MSG" placeholder="마음을 전하세요" value="<?php echo $userMSG; ?>" />
			</div>

		  <div class="form-group">
<label class="control-label">Profile Img.</label>
<input class="input-group" type="file" name="user_image" accept="image/*" />
			</div>

		  <div class="form-group">
<button type="submit" name="btnsave" class="btn btn-default">
        <span class="glyphicon glyphicon-save"></span> &nbsp; save
        </button>
			</div>

</form>
</div>
</body>
</html>
