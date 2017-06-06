# 파이게임 로딩하기
import pygame
import math
# 파이게임 시작하기
pygame.init()

# 색 정의
black=(0,0,0)
white=(255,255,255)
red=(255,0,0)
green=(0,255,0)
blue = (0,0,255)

pi=math.pi
sin=math.sin
cos=math.cos

sec_angle=0
min_angle=0
hour_angle=0
# 화면 띄우기
screen = pygame.display.set_mode([800, 600])

# 화면 타이틀 정의
pygame.display.set_caption('종민이의 첫게임')

# 게임루프 만들기
done = False
clock = pygame.time.Clock()
# Limit to 30 fps


while done == False:
    screen.fill(white)
    pygame.draw.ellipse(screen,blue,[175,75,450,450],5)
    pygame.draw.line(screen,green,[400,300],[400+150*sin(min_angle/360*2*pi),300-150*cos(min_angle/360*2*pi)],5)
    pygame.draw.line(screen,red,[400,300],[400+200*sin(sec_angle/360*2*pi),300-200*cos(sec_angle/360*2*pi)],5)
    pygame.draw.line(screen,blue,[400,300],[400+100*sin(hour_angle/360*2*pi),300-100*cos(hour_angle/360*2*pi)],5)
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            done = True
    if sec_angle<360:
        sec_angle += 6
    if sec_angle == 360:
        sec_angle = 0
        min_angle += 6
    if min_angle==360:
        min_angle=0
        hour_angle += 30

    pygame.display.flip()

    # pygame.draw.arc(screen,green,[20,220,250,200],0,pi/2,2)
    # pygame.draw.arc(screen,red,[20,220,600,600],0,pi/2,2)

# 화면 표시하기


    clock.tick(60)
# 게임종료하기
pygame.quit()
