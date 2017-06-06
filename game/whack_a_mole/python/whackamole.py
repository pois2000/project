import random
import pygame

black=(0,0,0)
white=(255,255,255)
red=(255,0,0)
green=(0,255,0)
blue = (0,0,255)
slot = [(10,150),(110,150),(210,150),
    (10,300),(110,300),(210,300),
    (10,450),(110,450),(210,450)]

window_width = 300
window_height = 500
frame_width = 70
frame_height = 103
px=py=w=h=score=total=0
prev_rect=pygame.Rect
game_speed = 600
level = 1

pygame.init()
screen = pygame.display.set_mode([window_width, window_height])
sound = [pygame.mixer.Sound('sound/papa.wav'),pygame.mixer.Sound('sound/mom.wav'), pygame.mixer.Sound('sound/min.wav'), pygame.mixer.Sound('sound/soo.wav'), pygame.mixer.Sound('sound/jun.wav')]
font = pygame.font.Font('font/ARCADECLASSIC.TTF', 25)

class MoleSprite(pygame.sprite.Sprite):
    """Define Mole Sprite"""
    def __init__(self):
        self.who = random.randint(0,4)
        self.mole = pygame.image.load("family_sprite.png").convert()
        self.mole.set_colorkey(white)
        self.images = []
        w, h = self.mole.get_size()
        for j in range(int(h/frame_height)):
            for i in range(int(w/frame_width)):
                self.images.append(self.mole.subsurface((i*frame_width,j*frame_height,frame_width,frame_height)))
        px, py = slot[random.randint(0,8)]
        self.rect = pygame.Rect(px+5,py-100,frame_width,h)

    def update(self):
        for i in [0,1,2,3,4]:
            screen.blit(self.images[self.who*5+i],self.rect)
            pygame.display.flip()
            pygame.time.delay(game_speed//10)
            draw_platform()
        pygame.time.delay(game_speed)

        for i in [4,3,2,1,0]:
            screen.blit(self.images[self.who*5+i],self.rect)
            pygame.display.flip()
            pygame.time.delay(game_speed//10)
            draw_platform()

    def soundplay(self):
        sound[self.who].play()

def drawslot(Surface,color):
    for i in range(len(slot)):
        x,y=slot[i]
        pygame.draw.ellipse(Surface, color, [x,y,80,30], 0)

def score_print():
    global score
    text = font.render("score "+str(score), True, red)
    screen.blit(text, (2, 2))

def missed_print():
    global total, score
    text = font.render("missed "+str(total-score), True, red)
    textobj = (window_width-text.get_rect().x-2,2)
    textobj = (170,2)
    screen.blit(text, textobj)

def level_print():
    global level
    text = font.render("LV"+str(level), True, red)
    textobj = (110,2)
    screen.blit(text, textobj)

def draw_platform():
    screen.fill(green)
    drawslot(screen, black)
    score_print()
    missed_print()
    level_print()

def main():
    global total,level,score,prev_rect, game_speed
    pygame.display.set_caption('Punch Seungwha Family!!')
    done = False
    score=total=0
    clock = pygame.time.Clock()
    pygame.mixer.music.load('sound/bg.mp3')
    pygame.mixer.music.play(10)

    while done == False:
        draw_platform()
        for event in pygame.event.get():
            if event.type == pygame.QUIT or total == 600:
                done = True
            if event.type == pygame.MOUSEBUTTONDOWN:
                if sprite.rect.collidepoint(pygame.mouse.get_pos()):
                    if sprite.rect == prev_rect:
                        prev_rect = sprite.rect
                    else :
                        sprite.soundplay()
                        score=score+1
                        prev_rect = sprite.rect

        sprite = MoleSprite()
        sprite.update()
        total = total + 1
        level = total//40+1
        game_speed = 600//level
        pygame.display.flip()
    pygame.quit()

# This isn't run on Android.
if __name__ == "__main__":
    main()
