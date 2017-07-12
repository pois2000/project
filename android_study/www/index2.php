<?php
    require_once 'dbconfig.php';
    $eventID = (string)$_GET['id'];

    if($eventID){
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
          $pin=$row['pin'];
          $hostName=$row['hostName'];
          $hostTel=$row['hostTel'];
          $purpose=$row['purpose'];
        }
      }

        $result2 = $DB_con->prepare("SELECT * FROM `tbl_users` WHERE  eventID = '".$eventID."'");
        $result2->execute();
        $count2 = $result->rowCount();
        if($count2 > 0)
        {
          while($row2=$result2->fetch(PDO::FETCH_ASSOC))
          {
            extract($row2);
            $images[]=$row2['userPic'];
            $sounds[]=$row2['userSound'];
            $names[]=$row2['userName'];
            $tels[]=$row2['userTel'];
            $msgs[]=$row2['message'];
            $userIDs[]=$row2['userID'];
          }
        }
      }
      else{
        header('refresh:0;index2.php?id=1000010000100001'); //기본 페이지 지정
        }
      $tel=preg_replace("/(0(?:2|[0-9]{2}))([0-9]+)([0-9]{4}$)/", "\\1-\\2-\\3", $calleeTel); //010-1234-1234 형식으로 표시하기
      $share_msg = "sms://?body=".rawurlencode($purpose."(".$hostName."요청) (".count($images)."참여) https://pois.000webhostapp.com/index.php?id=".$eventID."  [비번:".$pin."]");
?>
<html>

<head>
  <meta charset="utf-8">
  <title>
    <?php echo $calleeName."(".$tel.")"?>님의 러브롤</title>
  <link rel="stylesheet" href="style.css?v=<?=time();?>">
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
    <div id="menu">
    <a href="createnew.php" class="btn" target=_blank><i class="material-icons" style="font-size:35px">add_alert</i>새 럽롤</a>
    <a href=<?php echo $share_msg?> class="btn"><i class="material-icons" style="font-size:35px">share</i>친구 초대</a>
    <div class="scorebox"><div class="scorebox" id="score">0</div>
    <i class="material-icons" style="font-size:35px">face</i></div>
  </div>
    <div id="logo_message">
      <center>
        <span><?php echo $calleeName?>님의</span>
        <span lang=eng>LoveRoll</span>
      </center>
        <p><?php echo $purpose?><BR />
          우리 LoveRoll을 함께 해요!(<?php echo count($images)?>명 참여중)
        </p>
      </div>
  </div>
</div>

<!-- The Modal -->
<div id="myModal" class="modal">
  <div class="modal-content">
      <!-- <span class="close">&times;</span> -->
      <img id=modalimg >
      <div class="imgtxt">
        <p id=modalwho>이름</p>
        <p id=modaltxt>메시지</p>
      </div>
  </div>
</div>
      <div id="canvas">

      </div>
<div id=button>
  <a href="addnew.php?id=<?php echo $eventID?>" ><i class="material-icons" style="font-size:40px">add</i>글쓰기</a>
</div>

  <script>
    (function() {
      function getRandomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
      };

      var who = "<?php echo $calleeTel?>";
      var len= <?php echo count($images)?>;
      var images= [<?php echo '"'.implode('","', $images).'"' ?>];
      var sounds = [<?php echo '"'.implode('","', $sounds).'"' ?>];
      var game = (function() {
        var canvas;
        var score = 0;
        var running = 0;
        var moles = [];
        var moleCount=len;
        var hypes = [<?php echo '"'.implode('","', $names).'"' ?>];
        var tels = [<?php echo '"'.implode('","', $tels).'"' ?>];
        var msgs = [<?php echo '"'.implode('","', $msgs).'"' ?>];
        var userIDs = [<?php echo '"'.implode('","', $userIDs).'"' ?>];

        createClickHandler = function(i) {
          return function() {
            var mole = moles[i];
            if (mole.state == "alive") {
              score++;
              $("#score").html(score);
              mole.state = "dead";

              //play sound
              var audio = new Audio('user/' + who + '/sound/' + sounds[mole.imgid]).play();
              //상세보기 표시
              $("#modalimg").attr("src",'user/' + who + '/img/' + images[i]); //이미지 경로 정하기
              $("#modalwho").html(hypes[i]); //이름표시
              $("#modaltxt").html(msgs[i]); //내용 표시
              // $(".modal").css("display","block"); //보이게 할 수 있나?

              mole.element.removeClass("visible").addClass("dead");
              // mole.whack.addClass("visible");
              $(".modal").addClass("visible");
              mole.whackTtl = 500;
              mole.timer = getRandomInt(150, 3000);
              // mole.timer = getRandomInt(150, 3000);
            }
          }
        }
        init = function(parentId) {
          canvas = $("#" + parentId);
          for (var i = 0; i < moleCount; i++) {
            // Create mole container
            var moleContainer = $("<div class='mole_container'/>", {
              id: "MoleContainer_" + i
            });
            moleContainer.appendTo(canvas);

            // Create image
            var moleImg = $("<img></img>", {
              id: "Mole_" + i,
              class: 'mole hidden',
              src: 'user/' + who + '/img/' + images[i]
            })
            moleImg.bind('dragstart', function() {
              return false;
            });
            moleImg.click(createClickHandler(i));
            moleImg.appendTo(moleContainer);

            // Add a hype
            var molehype = $("<div class='hype'> <a href=sms://" + tels[i] + "?body=친구야%20LoveRoll%20고마워!%20%20"+ location.href + ">"+hypes[i] + "</a></div>", {
              id: "MoleHype" + i
            });
            molehype.appendTo(moleContainer);

            // Add a message
            var moleMsg = $("<div class='message'>" + msgs[i] + "</div>", {
              id: "MoleHype" + i
            });
            moleMsg.appendTo(moleContainer);
            //
            // Add a delete buttun
            var moleDel = $("<div class='del'><a href='del.php?delete_id=" + userIDs[i] + "'><i class=material-icons>clear</i></a></div>", {
            // var moleDel = $("<div class='del'><i class=material-icons>clear</i>"+ userIDs[i] + "</div>", {
              id: "MoleHype" + i
            });
            moleDel.appendTo(moleContainer);

            // Add the whack =)
            var whackImg = $("<img></img>", {
              id: "Whack_" + 1,
              class: 'whack',
              src: "image/whack.png"
            });
            whackImg.appendTo(moleContainer);
              // Create the mole
            var mole = {
              id: i,
              state: "dead",
              element: moleImg,
              imgid: i,
              whack: whackImg,
              whackTtl: 0,
              timer: getRandomInt(500, 3000)
            };
            moles.push(mole);

          }
        };
        start = function() {
          running = 1;
          delta = 0;
          lastFrameTimeMs = 0;
          requestAnimationFrame(gameloop);
        };
        stop = function() {
          running = 0;
        };
        update = function(step) {
          for (var i = 0; i < moleCount; i++) {
            var mole = moles[i];
            // Hide the whack
            if (mole.whackTtl > 0) {
              mole.whackTtl -= step;
              if (mole.whackTtl <= 0) {
                // mole.whack.removeClass("visible");
                $(".modal").removeClass("visible");
                mole.whackTtl = 0;
              }
            }
            // Mole lifetime events
            mole.timer -= step;
            if (mole.timer <= 0) {
              if (mole.state == "dead") {
                // mole.imgid = getRandomInt(0, len - 1);
                $("#Mole_" + mole.id).attr("src", 'user/' + who + '/img/' + images[mole.id]);
                // $("#Mole_" + mole.id).attr("src", 'user/' + who + '/img/' + images[mole.imgid]);
                mole.state = "alive";
                mole.element.removeClass("hidden").removeClass("dead").addClass("visible");
                mole.timer = getRandomInt(500, 3000);
              } else {
                mole.state = "dead";
                mole.element.removeClass("visible").addClass("hidden");
                mole.timer = getRandomInt(500, 3000);
              }
            }
          }
        };
        render = function() {};
        var timestep = 3000 / 25;
        var lastFrameTimeMs = 0;
        var delta = 0;
        gameloop = function(timestamp) {
          delta += timestamp - lastFrameTimeMs;
          lastFrameTimeMs = timestamp;
          while (delta >= timestep) {
            update(timestep);
            delta = -timestep;
          }
          //render();
          if (running)
            requestAnimationFrame(gameloop);
        };
        return {
          initialize: init,
          start: start,
          stop: stop
        };
  	})();
      $(function() {
        $(".mole").click(function() {
          $(this).removeClass("visible").addClass("hidden");
        });
        // $(".modal").click(function() {
        //   $(".modal").removeClass("visible");
        // });

        game.initialize("canvas");
        game.start();

      });


    })();

  </script>
</body>

</html>
