<?php
	// $tempnum = (string)$_GET['calleeTel'];
	error_reporting( ~E_NOTICE ); // avoid notice

	require_once 'dbconfig.php';
	// *** In	clude the class
	include("resize-class.php");
	include("find_file_end_num.php");
	include("thumbnail.php");
	$eventID = (string)$_GET['id'];

	$result = $DB_con->prepare("SELECT * FROM `event_table` WHERE eventID = '".$eventID."'");
	$result->execute();
	$count = $result->rowCount();
	if($count > 0)
	{
		while($row=$result->fetch(PDO::FETCH_ASSOC))
		{
			extract($row);
			$calleeTel=$row['calleeTel'];
			$calleeName=$row['calleeName'];
			$origine_pin=$row['pin'];
			$hostName=$row['hostName'];
			$hostTel=$row['hostTel'];
			$purpose=$row['purpose'];
		}
	}

	if(isset($_POST['btnsave']))
	{
		$userName = $_POST['user_name'];// user name
		$userTel = $_POST['user_tel'];// user tel
		$message = $_POST['message'];
		$get_pin = $_POST['pin'];
		$imgFile = $_FILES['user_pic']['name'];
		$tmp_dir = $_FILES['user_pic']['tmp_name'];
		$imgSize = $_FILES['user_pic']['size'];
		$sndFile = $_POST['user_sound'];

		if(empty($userName)){
			$errMSG = "당신의 이름을 입력해주세요.";
		}
		else if(empty($get_pin) or !is_numeric($get_pin) or strlen($get_pin)!=4){
			$errMSG = "비밀번호 숫자 4자리를 입력해주세요.";
		}
		else if($get_pin!=$origine_pin){
			$errMSG = "비밀번호가 다릅니다.";
		}
		else if(empty($imgFile)){
			$errMSG = "이미지를 선택해주세요";
		}
		// else if(empty($msg)){
		// 	$msg="";
		// }
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
			$sndExt = "wav"; // get sound extension
			// $sndExt = "wav"; // get sound extension

			// valid image extensions
			$valid_extensions = array('jpeg', 'jpg', 'png', 'gif'); // valid extensions

			$num = getFileNameList($upload_dir."/img")+1; //현재 디렉토리의  mole파일명 끝 찾기
			$num = str_pad($num, 4, '0', STR_PAD_LEFT); //파일 번호를 0000포맷으로 변경
			// rename uploading image
			$userPic = "mole".$num.".".$imgExt;
			$userSound = "mole".$num.".".$sndExt;

			if(in_array($imgExt, $valid_extensions)){
				// Check file size '5MB'
				if($imgSize < 10000000){

				move_uploaded_file($tmp_dir,$upload_dir."/img/".$userPic);
				rename("uploads/"."$calleeTel".".wav", $upload_dir."/sound/"."$userSound");  //서버에 저장된 파일 이동 및 이름 변경하기

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
					$errMSG = "10MB 이하 사진을 선택해주세요.";
				}
			}
			else{
				$errMSG = "JPG, JPEG, PNG & GIF 만 가능합니다.";
			}
		}


		// if no error occured, continue ....
		if(!isset($errMSG))
		{
			$stmt2 = $DB_con->prepare('INSERT INTO tbl_users(userName,userTel,userPic,userSound,eventID,message) VALUES(:uname, :utel, :upic, :usound, :eID, :msg)');
			$stmt2->bindParam(':uname',$userName);
			$stmt2->bindParam(':utel',$userTel);
			$stmt2->bindParam(':upic',$userPic);
			$stmt2->bindParam(':usound',$userSound);
			$stmt2->bindParam(':eID',$eventID);
			$stmt2->bindParam(':msg',$message);

		if($stmt2->execute())
		{
			$successMSG = "new record succesfully inserted ...";
			header("refresh:2;index.php?id=".$eventID); // redirects image view page after 5 seconds.
		}
		else
		{
			$errMSG = "error while inserting....";
		}

		}
	}
      $share_msg = "sms://?body=".rawurlencode($purpose."(".$hostName."요청) (".count($images)."참여) https://pois.000webhostapp.com/index.php?id=".$eventID."  [비번:".$pin."]");

?>

<html>
<head>
<title>  <?php echo $callee_name."(".$tel.")"?>님의 러브롤 참여 하기</title>
<meta charset="utf-8">
<link rel="shortcut icon" href="/favicon.ico" type="image/x-icon">
<link rel="icon" href="/favicon.ico" type="image/x-icon">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="style.css">
<script src="https://cdn.webrtc-experiment.com/RecordRTC.js"></script>
<!-- for Edige/FF/Chrome/Opera/etc. getUserMedia support -->
<script src="https://cdn.webrtc-experiment.com/gumadapter.js"></script>
<!-- <script src="audio_record.js"></script> -->
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.0/jquery.min.js"></script>
<link href="https://fonts.googleapis.com/css?family=Lobster" rel="stylesheet">
  <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">


</head>
<body>
  <div class="container" id="background-image">
	<div id="header">
    <div id="menu">
    <a href="createnew.php" class="btn" target=_blank><i class="material-icons" style="font-size:35px">add_alert</i>새 럽롤</a>
    <a href=<?php echo $share_msg?> class="btn"><i class="material-icons" style="font-size:35px">share</i>친구 초대</a>
  </div>
    <div id="logo_message">
			<center>
        <span><?php echo $calleeName?>님의</span>
        <span lang=eng>LoveRoll</span>
      </center>
        <p><?php echo $purpose."(".$hostName.")"?><BR />
          우리 LoveRoll을 함께 해요!
        </p>
      </div>
 </div>

	<?php
	if(isset($errMSG)){
			?>
            <div class="alert alert-danger">
							<i class="material-icons">info</i>
							<strong><?php echo $errMSG; ?></strong>
            </div>
            <?php
	}
	else if(isset($successMSG)){
		?>
        <div class="alert alert-success">
              <strong><i class="material-icons">info</i> <?php echo $successMSG; ?></strong>
        </div>
        <?php
	}

	?>

<form method="post" enctype="multipart/form-data">

	<div class="form-group">
		<label class="control-label"><i class="material-icons">fiber_pin</i>비밀번호</label>
		<input class="form-control" type="text" name="pin" placeholder="럽롤 참여 비밀번호 숫자 4자리" />
  </div>
	<div class="form-group">
		<label class="control-label"><i class="material-icons">account_circle</i>이름</label>
		<input class="form-control" type="text" name="user_name" placeholder="당신의 이름" />
  </div>

  <div class="form-group">
		<label class="control-label"><i class="material-icons">account_circle</i>번호(선택)</label>
		<input class="form-control" type="text" name="user_tel" placeholder="당신의 번호(선택)"  />
	</div>

	<div class="form-group">
		<label class="control-label"><i class="material-icons">add_a_photo</i> 축하 사진</label><br />
		<input class="input-group" type="file" name="user_pic" accept="image/*" />
  </div>

	<div class="form-group">
		<label class="control-label"><i class="material-icons">mode_comment</i>축하 메시지(선택)</label>
		<input class="form-control" type="text" name="message" placeholder="전하고 싶은 말 10글자(선택)"  />
  </div>

	<div class="form-group">
		<label class="control-label"><i class="material-icons">mic</i>축하 음성(선택)</label>
		<section class="experiment recordrtc">
				<button id="record" class="input-group">녹음(5초)</button>
				<select id="hidden" class="recording-media">
					<option value="record-audio">Audio</option>
				</select>
				<select id="hidden" class="media-container-format">
					<option>WAV</option>
				</select>
			<video id="hidden" controls muted></video> <!-- 여기가 녹음기? -->
		</section>
			<input id="hidden" class="user_sound" type="text" name="user_sound" />
</div>

	<div class="form-group">
		<button type="submit" name="btnsave" class="save_btn">
    	<i class="material-icons">done_all</i>저장
    </button>
	</div>
	<div id=hidden>
		<label class="control-label">친구 번호</label>
		<input id="ctel" class="form-control" type="text" name="callee_tel" value="<?php echo $calleeTel?>" placeholder="선물받을 사람 전화번호"  />
	</div>

</form>
</div>

    <script>
      (function() { // 뭔가 파일을 찾는 것 같은데..
        var params = {},
          r = /([^&=]+)=?([^&]*)/g;

        function d(s) {
          return decodeURIComponent(s.replace(/\+/g, ' '));
        }

        var match, search = window.location.search;
        while (match = r.exec(search.substring(1))) {
          params[d(match[1])] = d(match[2]);

          if (d(match[2]) === 'true' || d(match[2]) === 'false') {
            params[d(match[1])] = d(match[2]) === 'true' ? true : false;
          }
        }

        window.params = params;
      })();
    </script>

    <script> //여기가 선택시 값을 읽어 옴
      var recordingDIV = document.querySelector('.recordrtc'); //선택할 DIV 찾기
      var recordingMedia = recordingDIV.querySelector('.recording-media'); //select 영역 찾기
      var recordingPlayer = recordingDIV.querySelector('video'); //video 플레이어 찾기
      var mediaContainerFormat = recordingDIV.querySelector('.media-container-format'); //파일 포멧 찾기
      // recordingDIV.querySelector('button').onclick = function() { //녹음 버튼 누르면 실행
      recordingDIV.querySelector('button').onclick = function() { //녹음 버튼 누르면 실행
        var button = this;
        if (button.innerHTML === '녹음중지') {
          button.disabled = true;
          button.disableStateWaiting = true; //버튼 비활성화
          setTimeout(function() {
            button.disabled = false;
            button.disableStateWaiting = false;
          }, 5 * 1000); //재생중 5초뒤 녹음버튼이 가능하게 하네

          button.innerHTML = '녹음(5초)';
          function stopStream() {
            if (button.stream && button.stream.stop) {
              button.stream.stop();
              button.stream = null;
            }
          }

          if (button.recordRTC) { // 저장후 뭔가 처리함
            if (button.recordRTC.length) {
              button.recordRTC[0].stopRecording(function(url) {
                if (!button.recordRTC[1]) {
                  button.recordingEndedCallback(url);
                  stopStream();

                  saveToDiskOrOpenNewTab(button.recordRTC[0]);
                  return;
                }

                button.recordRTC[1].stopRecording(function(url) {
                  button.recordingEndedCallback(url);
                  stopStream();
                });
              });
            } else {
              button.recordRTC.stopRecording(function(url) {
                button.recordingEndedCallback(url);
                stopStream();

                saveToDiskOrOpenNewTab(button.recordRTC);
              });
            }
          }

          return;
        }

        button.disabled = true;

        var commonConfig = { // 녹음시작과 중단시 이벤트 처리
          onMediaCaptured: function(stream) {
            button.stream = stream;
            if (button.mediaCapturedCallback) {
              button.mediaCapturedCallback();
            }
            button.innerHTML = '녹음중지';
						$("#record").val("red");
            button.disabled = false;
            setTimeout(function() {
              // console.log("5초 타이머 완료");
              button.click(); //이제 중지시킨다
            }, 5 * 1000); //5초뒤 녹음 중지되면..

          },
          onMediaStopped: function() {
            button.innerHTML = '녹음(5초)';

            if (!button.disableStateWaiting) {
              button.disabled = false;
            }
          },
					onMediaCapturingFailed: function(error) {
						if (error.name === 'PermissionDeniedError' && !!navigator.mozGetUserMedia) {
							InstallTrigger.install({
								'Foo': {
									URL: 'https://addons.mozilla.org/en-US/firefox/addon/enable-screen-capturing/',
									toString: function() {
										return this.URL;
									}
								}
							});
						}

						commonConfig.onMediaStopped();
					}
					};

        if (recordingMedia.value === 'record-audio') { // 오디오 저장이 여기인듯..
          captureAudio(commonConfig);

          button.mediaCapturedCallback = function() { // 음악 포멧 세팅
            button.recordRTC = RecordRTC(button.stream, { // 음악 세팅?
              type: 'audio',
              bufferSize: typeof params.bufferSize == 'undefined' ? 0 : parseInt(params.bufferSize),
              sampleRate: typeof params.sampleRate == 'undefined' ? 44100 : parseInt(params.sampleRate),
              leftChannel: params.leftChannel || false,
              disableLogs: params.disableLogs || false,
              recorderType: webrtcDetectedBrowser === 'edge' ? StereoAudioRecorder : null
            });

            button.recordingEndedCallback = function(url) { //음악 저장 후처리
              var audio = new Audio();
              audio.src = url;
							console.log(audio.src);
              audio.controls = true;
              recordingPlayer.parentNode.appendChild(document.createElement('hr'));
              recordingPlayer.parentNode.appendChild(audio);

              if (audio.paused) audio.play();

              audio.onended = function() { //파일 저장 완료후 화면에 표시
                audio.pause();
                audio.src = URL.createObjectURL(button.recordRTC.blob);
              };
            };

            button.recordRTC.startRecording();
          };
        }
	}

      function captureAudio(config) { // 여기가 오디오 컨피그 유지
        captureUserMedia({
          audio: true
        }, function(audioStream) {
          recordingPlayer.srcObject = audioStream;
          recordingPlayer.play();
          config.onMediaCaptured(audioStream);

          audioStream.onended = function() {
            config.onMediaStopped();
          };
        }, function(error) {
          config.onMediaCapturingFailed(error);
        });
      }

      function captureUserMedia(mediaConstraints, successCallback, errorCallback) {// 여기가 진짜 녹음하는 곳
        navigator.mediaDevices.getUserMedia(mediaConstraints).then(successCallback).catch(errorCallback);
      }

      function setMediaContainerFormat(arrayOfOptionsSupported) { //선택시 값이 전달 됨
        var options = Array.prototype.slice.call(
          mediaContainerFormat.querySelectorAll('option')
        );
        var selectedItem;
        options.forEach(function(option) {
          option.disabled = true;

          if (arrayOfOptionsSupported.indexOf(option.value) !== -1) {
            option.disabled = false;

            if (!selectedItem) {
              option.selected = true;
              selectedItem = option;
            }
          }
        });
      }

      if (webrtcDetectedBrowser === 'edge') { //볼일 없음
        // webp isn't supported in Microsoft Edge
        // neither MediaRecorder API
        // so lets disable both video/screen recording options

        console.warn('Neither MediaRecorder API nor webp is supported in Microsoft Edge. You cam merely record audio.');

        recordingMedia.innerHTML = '<option value="record-audio">Audio</option>';
        setMediaContainerFormat(['WAV']);
      }

      if (webrtcDetectedBrowser === 'firefox') { //볼일 없음
        // Firefox implemented both MediaRecorder API as well as WebAudio API
        // Their MediaRecorder implementation supports both audio/video recording in single container format
        // Remember, we can't currently pass bit-rates or frame-rates values over MediaRecorder API (their implementation lakes these features)

        recordingMedia.innerHTML = '<option value="record-audio-plus-video">Audio+Video</option>' +
          '<option value="record-audio-plus-screen">Audio+Screen</option>' +
          recordingMedia.innerHTML;
      }

      // disabling this option because currently this demo
      // doesn't supports publishing two blobs.
      // todo: add support of uploading both WAV/WebM to server.
      if (false && webrtcDetectedBrowser === 'chrome') {
        recordingMedia.innerHTML = '<option value="record-audio-plus-video">Audio+Video</option>' +
          recordingMedia.innerHTML;
        console.info('This RecordRTC demo merely tries to playback recorded audio/video sync inside the browser. It still generates two separate files (WAV/WebM).');
      }

      function saveToDiskOrOpenNewTab(recordRTC) { //녹음후 바로 업로드
          if (!recordRTC) return alert('No recording found.');
          this.disabled = true;

          var button = this;
          uploadToServer(recordRTC, function(progress, fileURL) {
            if (progress === 'ended') {
								$(".user_sound").val($("#ctel").val()+".wav");
								var soundPath = $(".user_sound").val(); //값을 폼에 저장한다
              return;
            }
            button.innerHTML = progress;
          });
      }

      var listOfFilesUploaded = []; //생성된 파일이 있는 배열

      function uploadToServer(recordRTC, callback) { //서버 업로드
        var blob = recordRTC instanceof Blob ? recordRTC : recordRTC.blob;
        var fileType = blob.type.split('/')[0] || 'audio';
        var fileName = $("#ctel").val(); //파일명 정하기

        if (fileType === 'audio') {
          fileName += '.' + (!!navigator.mozGetUserMedia ? 'ogg' : 'wav');
        } else {
          fileName += '.webm';
        }

        // create FormData
        var formData = new FormData();
        formData.append(fileType + '-filename', fileName);
        formData.append(fileType + '-blob', blob);

        callback('Uploading ' + fileType + ' recording to server.');

        makeXMLHttpRequest('save.php', formData, function(progress) {
          if (progress !== 'upload-ended') {
            callback(progress);
            return;
          }

          var initialURL = location.href.replace(location.href.split('/').pop(), '') + 'uploads/';

          callback('ended', initialURL + fileName);

          // to make sure we can delete as soon as visitor leaves
          listOfFilesUploaded.push(initialURL + fileName);
        });
      }

      function makeXMLHttpRequest(url, data, callback) { //서버 연결함
        var request = new XMLHttpRequest();
        request.onreadystatechange = function() {
          if (request.readyState == 4 && request.status == 200) {
            callback('upload-ended');
          }
        };

        request.upload.onloadstart = function() {
          callback('Upload started...');
        };

        request.upload.onprogress = function(event) {
          callback('Upload Progress ' + Math.round(event.loaded / event.total * 100) + "%");
        };

        request.upload.onload = function() {
          callback('progress-about-to-end');
        };

        request.upload.onload = function() {
          callback('progress-ended');
        };

        request.upload.onerror = function(error) {
          callback('Failed to upload to server');
          console.error('XMLHttpRequest failed', error);
        };

        request.upload.onabort = function(error) {
          callback('Upload aborted.');
          console.error('XMLHttpRequest aborted', error);
        };

        request.open('POST', url);
        request.send(data);
      }

      window.onbeforeunload = function() { //화면을 빠져나가려고 할때 동작
        recordingDIV.querySelector('button').disabled = false;
        recordingMedia.disabled = false;
        mediaContainerFormat.disabled = false;

        if (!listOfFilesUploaded.length) return;

        listOfFilesUploaded.forEach(function(fileURL) {
          var request = new XMLHttpRequest();
          request.onreadystatechange = function() {
            if (request.readyState == 4 && request.status == 200) {
              if (this.responseText === ' problem deleting files.') {
                alert('Failed to delete ' + fileURL + ' from the server.');
                return;
              }

              listOfFilesUploaded = []; //녹음 파일 리스트를 지움
              // alert('You can leave now. Your files are removed from the server.');
            }
          };
          // request.open('POST', 'delete.php');
          var formData = new FormData(); //서버내 임시파일 지우기
          formData.append('delete-file', fileURL.split('/').pop());
          request.send(formData);
        });

        return 'Please wait few seconds before your recordings are deleted from the server.';
      };
    </script>
</body>
</html>