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

 //여기가 선택시 값을 읽어 옴
  var recordingDIV = document.querySelector('.recordrtc'); //선택할 DIV 찾기
  var recordingMedia = recordingDIV.querySelector('.recording-media'); //select 영역 찾기
  var recordingPlayer = recordingDIV.querySelector('video'); //video 플레이어 찾기
  var mediaContainerFormat = recordingDIV.querySelector('.media-container-format'); //파일 포멧 찾기
  // recordingDIV.querySelector('button').onclick = function() { //녹음 버튼 누르면 실행
  recordingDIV.querySelector('button').onclick = function() { //녹음 버튼 누르면 실행
    var button = this;
    if (button.innerHTML === '중지') {
      // console.log("중지 버튼 누름");
      button.disabled = true;
      button.disableStateWaiting = true; //버튼 비활성화
      setTimeout(function() {
        button.disabled = false;
        button.disableStateWaiting = false;
      }, 2 * 1000); //재생중 2초뒤 녹음버튼이 가능하게 하네

      button.innerHTML = '녹음';
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
        button.innerHTML = '중지';
        button.disabled = false;
        setTimeout(function() {
          console.log("2초 타이머 완료");
          button.click(); //이제 중지시킨다
        }, 2 * 1000); //2초뒤 녹음 중지되면..

      },
      onMediaStopped: function() {
        button.innerHTML = '녹음';

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
          audio.controls = true;
          recordingPlayer.parentNode.appendChild(document.createElement('hr'));
          recordingPlayer.parentNode.appendChild(audio);

          if (audio.paused) audio.play();

          audio.onended = function() { //파일 저장 완ㄹ후 화면에 표시
            audio.pause();
            audio.src = URL.createObjectURL(button.recordRTC.blob);
          };
        };

        button.recordRTC.startRecording();
      };
    }
  };

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
            console.log(fileURL); //파일 위치 표시
          return;
        }
        button.innerHTML = progress;
      });
  }

  var listOfFilesUploaded = [];

  function uploadToServer(recordRTC, callback) { //서버 업로드
    var blob = recordRTC instanceof Blob ? recordRTC : recordRTC.blob;
    var fileType = blob.type.split('/')[0] || 'audio';
    var fileName = (Math.random() * 1000).toString().replace('.', ''); //파일명 정하기

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

  window.onbeforeunload = function() { //녹음기 컨트롤
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

          listOfFilesUploaded = [];
          alert('You can leave now. Your files are removed from the server.');
        }
      };
      request.open('POST', 'delete.php');

      var formData = new FormData();
      formData.append('delete-file', fileURL.split('/').pop());
      request.send(formData);
    });

    return 'Please wait few seconds before your recordings are deleted from the server.';
  };
