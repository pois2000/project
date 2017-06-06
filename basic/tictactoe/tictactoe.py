# tic_tac_toe game
# 게임 순서 1. 그림을 그린다. 2. 선후를 정한다. 3. 입력을 받는다. 4. 판단하고 입력해준다. 5. 이겼는지 확인하고 아니라면 이길 방법을 내놓는다. 계속 돈다.
#  배치는 위에서 부터 1,2,3 | 4,5,6 | 7,8,9로 정의
# 최적 위치 찾는 방법은 모서리(1,3,7,9)를 먼저 선점, 센터 선점(5), 이후 모서리(2,4,6,8)
# 승자 판별은 (1,2,3),(4,5,6),(7,8,9),(1,4,7),(2,5,8),(3,6,9),(1,5,9),3,5,7)
def draw_board(board):
  b=board
  print("+---+---+---+")
  print("|",b[1],"|",b[2],"|",b[3],"|")
  print("+---+---+---+")
  print("|",b[4],"|",b[5],"|",b[6],"|")
  print("+---+---+---+")
  print("|",b[7],"|",b[8],"|",b[9],"|")
  print("+---+---+---+\n")

def whoisfirst():
  turn=""
  while turn not in ["Y","N"]:
    turn=input("틱택토 게임을 하겠습니다.\n먼저 하시겠습니까? (Y/N)").upper()
  else:
    if turn=="Y":
      return ('o','x')   #사람이 "O", 컴퓨터가 "x" 로 표시
    else:
      return ('x','o')  #사람이 "x", 컴퓨터가 "O" 로 표시

def get_position():
  p=100
  while p not in range(1,10):
    p=int(input("숫자를 선택하세요:"))
  return p

def is_blank(position, board):
  if board[position] in range(10):
    return True
  else:
    return False

def make_list(mark, board): #어떤 표시가 있는 위치를 찾기
  list=[]
  for i in range(1,10):
    if board[i] == mark:
      list.append(i)
  return list

def get_best_position(mark, board):  #컴퓨터의 자리 찾기
  # my_list=[]     #내 위치 리스트
  blank_list=[]  #빈자리 리스트

  first_position=[1,3,7,9]
  second_position=[5]
  third_position=[2,4,6,8]

# 나, 상대방 마크 찾기
  if mark=='o':
    your_mark='x'
  else :
    your_mark ='o'

  # my_list=make_list(mark, board) #내 위치 파악

  # 빈자리 찾기
  for i in range(1,10):
    if is_blank(i, board):
      blank_list.append(i)

  # 승리 위치 찾기
  for i in blank_list:
    test_board = board[:]
    test_board[i]=mark
    if is_winner(test_board): # 나의 승리 수를 찾기
      return i
  for i in blank_list:
    test_board = board[:]
    test_board[i]=your_mark
    if is_winner(test_board): # 상대방 승리 수를 먼저 찾아 막기
      return i
  for i in first_position:
    if blank_list.count(i):
      return i
  for i in second_position:
    if blank_list.count(i):
      return i
  for i in third_position:
    if blank_list.count(i):
      return i

def is_winner(board):
    b=board
    if ((b[1]==b[2] and b[2] == b[3]) or
    	(b[4]==b[5] and b[5] == b[6]) or
    	(b[7]==b[8] and b[8] == b[9]) or
    	(b[1]==b[4] and b[4] == b[7]) or
    	(b[2]==b[5] and b[5] == b[8]) or
    	(b[3]==b[6] and b[6] == b[9]) or
    	(b[1]==b[5] and b[5] == b[9]) or
    	(b[3]==b[5] and b[5] == b[7])):
      return True
    else:
      return False

def is_full(board):
  result=False
  for i in range(1,10):
    result += is_blank(i,board)
  return result

def play_again():
  while True:
    play_again=input("다시 하시겠습니까?(Y/N)").upper()
    if play_again =="N":
      return False
    elif play_again =="Y":
      return True

go_stop=1 # go: 1, stop 0

while go_stop:
  board=[0,1,2,3,4,5,6,7,8,9] #보드 배열
  human_mark, computer_mark = whoisfirst()# human or computer로 받음
  human_position=0
  if human_mark =='o':
    turn='human'
  else:
    turn='computer'

  while is_full(board):
    draw_board(board)

    if turn=='human':
      human_position=get_position()
      while is_blank(human_position, board)==False:
        print("빈자리가 아닙니다")
        draw_board(board)
        human_position=get_position()
      else:
        board[human_position]=human_mark
        if is_winner(board):
          draw_board(board)
          print("당신이 승리했습니다!!")
          break
      turn="computer"

    if turn == "computer":
      if is_full(board):
        computer_position=get_best_position(computer_mark, board)
        board[computer_position]=computer_mark
        print('컴퓨터가 %d에 뒀습니다.'% computer_position)
        if is_winner(board):
          draw_board(board)
          print("당신이 졌어요!!")
          break
      else:
        break
      turn="human"

  go_stop=play_again()
