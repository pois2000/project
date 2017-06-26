<?php
function pngresize($filepath ,$new_width, $new_height) {
  list($o_width, $o_height) = getimagesize($filepath);
  if($o_width>$new_width && $o_height>$new_height)
    {
        $original_aspect = $o_width / $o_height;
        $new_aspect = $new_width / $new_height;
        if ( $original_aspect >= $new_aspect )
        {
           // If image is wider than newnail (in aspect ratio sense)
           $new_height = $new_height;
           $new_width = $o_width / ($o_height / $new_height);
        }
        else
        {
           // If the newnail is wider than the image
           $new_width = $new_width;
           $new_height = $o_height / ($o_width / $new_width);
        }

        $image_p = imagecreatetruecolor($new_width, $new_height);
        imagealphablending($image_p, false);
        imagesavealpha($image_p, true);
        $image = imagecreatefrompng($filepath);
        imagecopyresampled($image_p, $image, 0, 0 - ($new_width - $new_width) / 2, 0 - ($new_height - $new_height) / 2, 0, $new_width, $new_height, $o_width, $o_height);
        imagepng($image_p,$filepath,5);
    }
}
?>
