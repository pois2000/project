import cv2
# from itertools import count

face_cascade = cv2.CascadeClassifier('data/haarcascade_frontalface_alt.xml')
eye_cascade = cv2.CascadeClassifier('data/haarcascade_eye_tree_eyeglasses.xml')
# Mouth_cascade = cv2.CascadeClassifier('data/Mouth.xml')

# Load the overlay image: glasses.png
imgGlasses = cv2.imread('data/heart.png')
# imgMouth = cv2.imread('data/mustache.png')
# #Check if the files opened
# if  imgGlasses is None :
#     exit("Could not open the image")
# if  face_cascade.empty() :
#     exit("Missing: haarcascade_frontalface_default.xml")
# if  eye_cascade.empty() :
#     exit("Missing: haarcascade_eye.xml")


# Create the mask for the glasses
imgGlassesGray = cv2.cvtColor(imgGlasses, cv2.COLOR_BGR2GRAY)
ret, origGlasses_mask = cv2.threshold(imgGlassesGray, 10, 255, cv2.THRESH_BINARY)
origGlasses_mask_inv = cv2.bitwise_not(origGlasses_mask)

# Convert glasses image to BGR
# and save the original image size (used later when re-sizing the image)
imgGlasses = imgGlasses[:,:,0:3]
origGlassesHeight, origGlassesWidth = imgGlasses.shape[:2]

# # Create the mask for the Mouth
# imgMouthGray = cv2.cvtColor(imgMouth, cv2.COLOR_BGR2GRAY)
# ret, origMouth_mask = cv2.threshold(imgMouthGray, 10, 255, cv2.THRESH_BINARY)
# origMouth_mask_inv = cv2.bitwise_not(origMouth_mask)
#
# # Convert Mouth image to BGR
# # and save the original image size (used later when re-sizing the image)
# imgMouth = imgMouth[:,:,0:3]
# origMouthHeight, origMouthWidth = imgMouth.shape[:2]



video_capture = cv2.VideoCapture(0)

if not video_capture.isOpened() :
    exit('The Camera is not opened')


# counter = count(1)
#
while True:
    # print ("Iteration %s" % counter)
    ret, frame = video_capture.read()
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = face_cascade.detectMultiScale(gray, 1.3, 5)
    for (x,y,w,h) in faces:
        # cv2.rectangle(frame,(x,y),(x+w,y+h),(255,0,0),2)
        roi_gray = gray[y:y+h, x:x+w]
        roi_color = frame[y:y+h, x:x+w]
        eyes = eye_cascade.detectMultiScale(roi_gray)
        # Mouth = Mouth_cascade.detectMultiScale(roi_gray)
        #cv2.imshow('Video', roi_gray)
        #cv2.waitKey()

        #print 'X:%i, Y:%i, W:%i, H:%i' % (x, y, w, h)
        # for (ex,ey,ew,eh) in eyes:
        #     cv2.rectangle(roi_color,(ex,ey),(ex+ew,ey+eh),(0,255,0),2)
            # print ('EX:%i, EY:%i, EW:%i, EH:%i' % (ex, ey, ew, eh))
        for (ex, ey, ew, eh) in eyes:
            scale=1.5
            glassesWidth = ew*scale
            glassesHeight = glassesWidth * origGlassesHeight / origGlassesWidth

            # Center the glasses on the Center of the eyes

            x1 = int(ex + ew/2 - glassesWidth/2)
            x2 = int(ex + ew/2 + glassesWidth/2)
            y1 = int(ey + eh/2 - glassesHeight/2)
            y2 = int(ey + eh/2 + glassesHeight/2)

                # Check for clipping
            if x1 < 0:
                x1 = 0
            if y1 < 0:
                y1 = 0
            if x2 > w:
                x2 = w
            if y2 > h:
                y2 = h

            # Re-calculate the width and height of the glasses image
            glassesWidth = int(x2 - x1)
            glassesHeight = int(y2 - y1)

            # Re-size the original image and the masks to the glasses sizes
            # calcualted above
            glasses = cv2.resize(imgGlasses, (glassesWidth,glassesHeight), interpolation = cv2.INTER_AREA)
            mask = cv2.resize(origGlasses_mask, (glassesWidth,glassesHeight), interpolation = cv2.INTER_AREA)
            mask_inv = cv2.resize(origGlasses_mask_inv, (glassesWidth,glassesHeight), interpolation = cv2.INTER_AREA)

            # take ROI for glasses from background equal to size of glasses image
            roi = roi_color[y1:y2, x1:x2]

            # roi_bg contains the original image only where the glasses is not
            # in the region that is the size of the glasses.
            roi_bg = cv2.bitwise_and(roi,roi,mask = mask_inv)

            # roi_fg contains the image of the glasses only where the glasses is
            roi_fg = cv2.bitwise_and(glasses,glasses,mask = mask)

            # join the roi_bg and roi_fg
            dst = cv2.add(roi_bg,roi_fg)

            # place the joined image, saved to dst back over the original image
            roi_color[y1:y2, x1:x2] = dst

        # for (ex, ey, ew, eh) in Mouth:
        #     scale=1.3
        #     MouthWidth = ew*scale
        #     MouthHeight = MouthWidth * origMouthHeight / origMouthWidth
        #
        #     # Center the Mouth on the Center of the eyes
        #
        #     x1 = int(ex + ew/2 - MouthWidth/2)
        #     x2 = int(ex + ew/2 + MouthWidth/2)
        #     y1 = int(ey + eh/2 - MouthHeight/2)
        #     y2 = int(ey + eh/2 + MouthHeight/2)
        #
        #         # Check for clipping
        #     if x1 < 0:
        #         x1 = 0
        #     if y1 < 0:
        #         y1 = 0
        #     if x2 > w:
        #         x2 = w
        #     if y2 > h:
        #         y2 = h
        #
        #     # Re-calculate the width and height of the Mouth image
        #     MouthWidth = int(x2 - x1)
        #     MouthHeight = int(y2 - y1)
        #
        #     # Re-size the original image and the masks to the Mouth sizes
        #     # calcualted above
        #     Mouth = cv2.resize(imgMouth, (MouthWidth,MouthHeight), interpolation = cv2.INTER_AREA)
        #     mask = cv2.resize(origMouth_mask, (MouthWidth,MouthHeight), interpolation = cv2.INTER_AREA)
        #     mask_inv = cv2.resize(origMouth_mask_inv, (MouthWidth,MouthHeight), interpolation = cv2.INTER_AREA)
        #
        #     # take ROI for Mouth from background equal to size of Mouth image
        #     roi = roi_color[y1:y2, x1:x2]
        #
        #     # roi_bg contains the original image only where the Mouth is not
        #     # in the region that is the size of the Mouth.
        #     roi_bg = cv2.bitwise_and(roi,roi,mask = mask_inv)
        #
        #     # roi_fg contains the image of the Mouth only where the Mouth is
        #     roi_fg = cv2.bitwise_and(Mouth,Mouth,mask = mask)
        #
        #     # join the roi_bg and roi_fg
        #     dst = cv2.add(roi_bg,roi_fg)
        #
        #     # place the joined image, saved to dst back over the original image
        #     roi_color[y1:y2, x1:x2] = dst
    #break
    #Display the resulting frame
    cv2.imshow('Video', frame)
    # cv2.waitKey()
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# When everything is done, release the capture
video_capture.release()
cv2.destroyAllWindows()
