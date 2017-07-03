<?php
    require_once 'dbconfig.php';
    $who = (string)$_GET['who'];
    if($who){
        $result = $DB_con->prepare("SELECT * FROM `tbl_users` WHERE calleeTel = '".$who."'");
        $result->execute();
        $count = $result->rowCount();
        if($count > 0)
        {
          while($row=$result->fetch(PDO::FETCH_ASSOC))
          {
            extract($row);
            $images[]=$row['userPic'];
            $sounds[]=$row['userSound'];
            $names[]=$row['userName'];
            // $calleeName[]=$row['calleeName'];
            // $tels[]=$row['userTel'];

          }
        }
      }
      else{
        header('refresh:0;index.php?who=01042583351');
        }
      $tel=preg_replace("/(0(?:2|[0-9]{2}))([0-9]+)([0-9]{4}$)/", "\\1-\\2-\\3", $who);
?>

<html>

<head>
  <meta charset="utf-8">
  <title>
    <?php echo $tel?>님의 러브롤</title>
  <link rel="stylesheet" href="style.css?v=<?=time();?>">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.0/jquery.min.js"></script>
  <link href="https://fonts.googleapis.com/css?family=Lobster" rel="stylesheet">

</head>
<script>
var _showPage = $(function() {
  var loader = $("div.loader");
  var container = $("div.container");
  loader.css("display","none");
  container.css("display","block");
})();
</script>
<body>
  <div class="loader"></div>
  <div class="container" id="background-image">
    <div id="header">
      <div id="btn_group">
        <a href="#" class="btn">Reply</a>
        <a href="#" class="btn">Share</a>
        <a href="addnew.php" class="btn" target=new>New!</a>
      </div>
      <div id="scorebox">SCORE <span class="score">0</span>
      </div>
      <div id="logo_message">
        <h1>LoveRoll</h1>
        <p><?php echo $tel?>님<br />
        친구들이 보내온 LoveRoll을 눌러 보세요!</p>
    </div>
  </div>
  <div id="main" class="content">
      <div id="canvas">
    </div>
  </div>
</div>

  <script>
    (function() {
      function getRandomInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
      };

      var who = "<?php echo $who?>";
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

        createClickHandler = function(i) {
          return function() {
            var mole = moles[i];
            if (mole.state == "alive") {
              score++;
              $("#score").html(score);
              mole.state = "dead";
              //play sound
              new Audio('user/' + who + '/sound/' + sounds[mole.imgid]).play();
              mole.element.removeClass("visible").addClass("dead");
              mole.whack.addClass("visible");
              mole.whackTtl = 150;
              mole.timer = getRandomInt(1000, 6000);
            }
          };
        };
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
            var molehype = $("<div class='hype'>" + hypes[i] + "</div>", {
              id: "MoleHype" + i
            });
            molehype.appendTo(moleContainer);

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
              timer: getRandomInt(1000, 2000)
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
                mole.whack.removeClass("visible");
                mole.whackTtl = 0;
              }
            }
            // Mole lifetime events
            mole.timer -= step;
            if (mole.timer <= 0) {
              if (mole.state == "dead") {
                mole.imgid = getRandomInt(0, len - 1);
                $("#Mole_" + mole.id).attr("src", 'user/' + who + '/img/' + images[mole.imgid]);
                mole.state = "alive";
                mole.element.removeClass("hidden").removeClass("dead").addClass("visible");
                mole.timer = getRandomInt(250, 3000);
              } else {
                mole.state = "dead";
                mole.element.removeClass("visible").addClass("hidden");
                mole.timer = getRandomInt(250, 5000);
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
        game.initialize("canvas");
        game.start();
      });
    })();

  </script>
</body>

</html>
