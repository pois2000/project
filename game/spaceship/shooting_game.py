# -*- coding: utf-8 -*-

import pygame
import random
import sys

# Define some colors
BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
RED = (255, 0, 0)
BLUE = (0, 0, 255)
GREEN = (0, 255, 0)

screen_width = 700
screen_height = 400

background_position = [0, 0]
# --- Classes

class Block(pygame.sprite.Sprite):
    """ This class represents the block. """
    def __init__(self, color):
        # Call the parent class (Sprite) constructor
        super().__init__()

        num=random.randint(1,9)
        temp = pygame.image.load("image/spaceAstronauts_00"+str(num)+".png").convert()
        self.image = pygame.transform.rotate(temp,-90)
        w,h = self.image.get_size()
        scale=random.randint(10,100)/100
        self.image = pygame.transform.scale(self.image, (int(w*scale), int(h*scale)))

        self.image.set_colorkey(BLACK)
        self.rect = self.image.get_rect()

    def reset_pos(self):
        """ Reset position to the top of the screen, at a random x location.
        Called by update() or the main program loop if there is a collision.
        """
        self.rect.y = random.randrange(-300, -20)
        self.rect.x = random.randrange(0, screen_width)

    def update(self):
        """ Move the block. """
        self.rect.y += 1

        if self.rect.y > 410:
            self.reset_pos()

class Player(pygame.sprite.Sprite):
    """ This class represents the Player. """
    def __init__(self):
        super().__init__()
        self.image = pygame.image.load("image/player.png").convert()
        w,h = self.image.get_size()
        scale=.5
        self.image = pygame.transform.scale(self.image, (int(w*scale), int(h*scale)))
        self.image.set_colorkey(BLACK)
        self.rect = self.image.get_rect()
        self.rect.x = random.randrange(0, screen_width)

    def update(self):
        """ Update the player's position. """
        if self.rect.x > 676:
            self.rect.x = 676
        if self.rect.x < -27:
            self.rect.x = -27

class Bullet(pygame.sprite.Sprite):
    """ This class represents the bullet . """
    def __init__(self):
        # Call the parent class (Sprite) constructor
        super().__init__()
        num=random.randint(1,9)
        self.image = pygame.image.load("image/spaceMissiles_00"+str(num)+".png").convert()
        w,h = self.image.get_size()
        scale=1
        self.image = pygame.transform.scale(self.image, (int(w*scale), int(h*scale)))
        self.image.set_colorkey(BLACK)
        self.rect = self.image.get_rect()

    def update(self):
        """ Move the bullet. """
        self.rect.y -= 3

# Initialize Pygame
pygame.init()

# Set the height and width of the screen
# 2) 화면 해상도를 480*320, 전체 화면 모드, 하드웨어 가속 사용, 더블 버퍼 모드로 초기화하는 경우

screen = pygame.display.set_mode([screen_width, screen_height])

click_sound = pygame.mixer.Sound("sound/laser5.ogg")
pop_sound = pygame.mixer.Sound("sound/BalloonPopping.wav")
bomb_sound = pygame.mixer.Sound("sound/SwordSwing.wav")

# --- Sprite lists

# This is a list of every sprite. All blocks and the player block as well.
all_sprites_list = pygame.sprite.Group()

# List of each block in the game
block_list = pygame.sprite.Group()

# List of each bullet
bullet_list = pygame.sprite.Group()

# --- Create the sprites
background_image = pygame.image.load("image/space.jpeg").convert()


for i in range(50):
    # This represents a block
    block = Block(BLUE)

    # Set a random location for the block
    block.rect.x = random.randrange(screen_width)
    block.rect.y = random.randrange(300)

    # Add the block to the list of objects
    block_list.add(block)
    all_sprites_list.add(block)

# Create a red player block
player = Player()
all_sprites_list.add(player)

# Loop until the user clicks the close button.
done = False

# Used to manage how fast the screen updates
clock = pygame.time.Clock()

score = 0
player.rect.y = 350

# -------- Main Program Loop -----------
while not done:
    # --- Event Processing
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            done = True
    keys = pygame.key.get_pressed()
    if keys[pygame.K_LEFT]:
        player.rect.x -= 10

    if keys[pygame.K_RIGHT]:
        player.rect.x += 10

    if keys[pygame.K_SPACE]:
        bullet = Bullet()
        click_sound.play()
        # Set the bullet so it is where the player is
        bullet.rect.x = player.rect.x+38/2
        bullet.rect.y = player.rect.y
        # Add the bullet to the lists
        all_sprites_list.add(bullet)
        bullet_list.add(bullet)

    # --- Game logic

    # Call the update() method on all the sprites
    all_sprites_list.update()

    # Calculate mechanics for each bullet
    for bullet in bullet_list:

        # See if it hit a block
        block_hit_list = pygame.sprite.spritecollide(bullet, block_list, True)
        # bomb_sound.play()
        # For each block hit, remove the bullet and add to the score
        for block in block_hit_list:
            bullet_list.remove(bullet)
            all_sprites_list.remove(bullet)
            score += 1

        # Remove the bullet if it flies up off the screen
        if bullet.rect.y < -10:
            bullet_list.remove(bullet)
            all_sprites_list.remove(bullet)

    # See if it hit a block
    if pygame.sprite.spritecollide(player, block_list, True):
        # font = pygame.font.SysFont('GULIM.TTF', 25, True, False)
        # text = font.render("kooong!!",True, RED)
        # screen.blit(text, [100, 100])
        # pygame.time.wait(500)
        pop_sound.play()

    if len(block_list) == 0:
        for i in range(50):
            # This represents a block
            block = Block(BLUE)

            # Set a random location for the block
            block.rect.x = random.randrange(screen_width)
            block.rect.y = random.randrange(-300,50)

            # Add the block to the list of objects
            block_list.add(block)
            all_sprites_list.add(block)
        # print(score%50)
    # --- Draw a frame

    screen.blit(background_image, background_position)
    # Draw all the spites
    font = pygame.font.SysFont('GULIM.TTF', 25, True, False)
    text = font.render("Score:"+str(score),True, GREEN)
    screen.blit(text, [0, 0])

    all_sprites_list.draw(screen)

    # Go ahead and update the screen with what we've drawn.
    pygame.display.flip()

    # --- Limit to 20 frames per second
    clock.tick(60)

pygame.quit()
