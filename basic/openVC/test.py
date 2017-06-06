import numpy as np
import cv2

cap = cv2.VideoCapture(0)
im = cv2.imread('heart.png')
height, width = im.shape[:2]

while(True):
    # Capture frame-by-frame
    ret, frame = cap.read()
    # Our operations on the frame come here
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    thumbnail = cv2.resize(im, (int(width/10), int(height/10)), interpolation = cv2.INTER_AREA)
    # Display the resulting frame
    cv2.imshow('frame',gray)
    cv2.imshow('frame',thumbnail)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# When everything done, release the capture
cap.release()
cv2.destroyAllWindows()
