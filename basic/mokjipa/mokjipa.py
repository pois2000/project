# 가위바위보 오브젝트의 파이선 코드

import random

def getnum():
  human=int(input("가위 1, 바위 2 보 3을 누르면 게임을 하게 되. 0을 누르면 끝이야."))
  return human

def iswhat(num):
  if num == 1 :
    return "가위"
  elif num == 2 :
    return "바위"
  elif num == 3 :
    return "보"

def whowin(human, com):
  # win = [(2,1),(3,2),(1,3)]
  # lose= [(1,2),(2,3),(3,1)]
  who=(human,com)
  if human == com :
    print("비겼네요!\n")
  elif win.count(who):
    print("당신이 이겼어요!\n")
  elif lose.count(who):
    print("당신이 졌어요!\n")

com = 0
human = 0
print("가위바위보를 해봐요.")

human=getnum()
while human:
  com = random.randint(1, 3)
  print("사람", iswhat(human), "컴퓨터",iswhat(com))
  whowin(human, com)
  human=getnum()
