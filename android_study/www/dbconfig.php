<?php

	$DB_HOST = "localhost";
	// $DB_HOST = "mysql.hostinger.kr";
	$DB_USER = "id2006562_pois";
	// $DB_USER = "u984921315_pois";
	$DB_PASS = "shekwl";
	// $DB_PASS = "shekwl11";
	$DB_NAME = "id2006562_table_test";
	// $DB_NAME = "u984921315_test";


	try{
		$DB_con = new PDO("mysql:host={$DB_HOST};dbname={$DB_NAME}",$DB_USER,$DB_PASS);
		$DB_con->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
	}
	catch(PDOException $e){
		echo $e->getMessage();
	}
?>
