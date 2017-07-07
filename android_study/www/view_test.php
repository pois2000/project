<?php
  	require_once 'dbconfig.php';
    $who = (string)$_GET['who'];
    if($who){
        $result = $DB_con->prepare("SELECT * FROM `media_table` WHERE calleeTel = '".$who."'");
        $result->execute();
        $count = $result->rowCount();
        if($count > 0)
      	{
      		while($row=$result->fetch(PDO::FETCH_ASSOC))
      		{
            extract($row);
            $array[]=$row;
          }
        }
        // print_r($array);
        echo json_encode($array);
      }
?>
