import sys, os, time, requests, re, json
from PyQt5.QtWidgets import QWidget, QDesktopWidget, QApplication, QMainWindow, QTableWidgetItem, QLabel, QComboBox, QTableWidget, QHeaderView, QVBoxLayout, QHBoxLayout, QProgressBar
from PyQt5.QtGui import QImage, QPixmap, QBrush, QColor, QPalette, QPainter, QIcon
from PyQt5.QtCore import QThread, pyqtSignal, pyqtSlot, QTimer, Qt, QDir, QUrl
# from PyQt5.QtChart import QCandlestickSeries, QCandlestickSet, QChart, QDateTimeAxis, QValueAxis, QChartView
# from PyQt5.QtWebEngineWidgets import QWebEngineView
from websocket import *
from tqdm import tqdm
import pandas as pd
from pandas import DataFrame as df
from pandas import Series
from datetime import datetime, timedelta
from bs4 import BeautifulSoup
from urllib.parse import unquote
import webbrowser
import matplotlib.pyplot as plt
from matplotlib.backends.backend_qt5agg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.figure import Figure
import mplfinance as mpf

pd.options.display.float_format = '{:.2f}'.format

def isnum(value):
    try:
        v=int(value)
        return format(value,",")
    except:
        return str(value)
# 코인 이름 찾기
def getCoinName():  #함수로 만들어 줍니다.
    coin_list = requests.get("https://api.upbit.com/v1/market/all") #속성이 필요없어 간단하게 한 줄로 합칩니다.
    coin_list = df(coin_list.json()) #받은 데이터를 json -> dataframe으로 변경합니다.
    coin_list = coin_list[coin_list.market.str.contains(pat='KRW', regex=False)]
    return coin_list

#업비트 현재 시세판 만들기
def getSise(coin_list):
    url = "https://api.upbit.com/v1/ticker"
    querystring = {"markets":",".join(coin_list)}
    res = requests.request("GET", url, params=querystring)
    res=df(json.loads(res.text))

    res=res[['market','trade_price','signed_change_price','signed_change_rate','acc_trade_price_24h']]  # 필요한 컬럼만 남기기
    res = res.sort_values(by='acc_trade_price_24h', ascending= False) # 거래량 내림차순으로 정렬
    res = res.reset_index(drop=True) # 인덱스를 재정열
    res.signed_change_rate = (res.signed_change_rate*10000).astype(int)/100 # 보기쉽게 %로 만들기
    res.acc_trade_price_24h //=100000000 # 보기쉽게 억단위로로 만들기
    res=res.rename(columns = {'market' : '이름', 'trade_price' : '현재가','signed_change_price' : '변동(원)',
        'signed_change_rate' : '변동(%)','acc_trade_price_24h' : '거래(억원)',}) # 읽기 쉽게 한글로 바꾸기
    return res

#분당 데이터 가져오기 - 무제한이며 극강임, 마지막 행은 현재 시세 포함임
def getData(code, intv=5, count=200):
    if code=="KRW-KRW":
        return
    elif "-" not in code:
        code="KRW-"+code
    cd = code.split("-")[1]+code.split("-")[0]

    if intv<1440: iv=intv
    elif intv==1440: iv='1D'
    elif intv==1440*7: iv='1W'
    elif intv==1440*30: iv='1M'
    cnt = int(count//1500+1) # 최대 시행횟수
    now = datetime.now()
    start_time = now - timedelta(minutes=count*intv) #최종 목표 시간
    s_t = start_time

    end_time = now
    res = df()
    for i in range(cnt):
        e_t = s_t + timedelta(minutes=(intv*1500))
        f=int(time.mktime((s_t).timetuple()))
        e=int(time.mktime((e_t).timetuple()))
        url=f'https://crix-api-tv.upbit.com/v1/crix/tradingview/history?symbol={cd}&resolution={iv}&from={f}&to={e}'
        try:
            r=requests.request("GET", url).text
        except:
            time.sleep(3)
            r=requests.request("GET", url).text

        if "no_data" in str(r):
            print("데이터 없음")
            break
        else:
            d = df(json.loads(r))
            s_time = datetime.fromtimestamp(d.t.min()/1000) #DB 시작 시간
            s_t = e_time = datetime.fromtimestamp(d.t.max()/1000) #DB 끝 시간
    #         print("받은거 시작", s_time, "끝", e_time)
            res = pd.concat([res, d])
    if res.shape[0]>0:
        # res['Date']=res.t
        res['Date']=res.t.apply(lambda x :  x + 9 * 3600000)
        res = res.rename(columns = {'v': 'Volume', 'o': 'Open', 'h': 'High', 'l': 'Low', 'c': 'Close','t':'timestamp'})
        res=df(res, columns=['timestamp','Date','Open', 'High', 'Low', 'Close','Volume']) #열순서변경
        res = res.drop_duplicates()  # 중복제거
        if intv<1440:
            res.Date=res.Date.apply(lambda x: datetime.fromtimestamp(x/1000)-timedelta(hours=9))
        else:
            res.Date=res.Date.apply(lambda x: datetime.fromtimestamp(x/1000)-timedelta(hours=18))
        res = res.sort_values(['Date'], ascending=True)
        res = res.loc[res.Date>=datetime.now()-timedelta(minutes=intv*count)]
        res = res.reset_index(drop=True)
    return res

#코인 뉴스 및 공지를 가져옴
def getCoinNews(code='bch', days=7, news=True):
    sw=0 #코드가 들어오면 1
    if code.lower()!='all' and len(code.split("-"))>1: #KRW-BTC처럼 코드로 받을 경우 BTC만 남기기 위함임
        code=code.split("-")[-1]
        sw=1
    elif code.lower()!='all' and "_" not in code:
        sw=1
    if news==True:

        #코인 뉴스를 구글에서 검색해서 가져옴
        url=f'https://www.google.com/search?q={code}+코인&tbm=nws'
        html=requests.get(url).text
        soup = BeautifulSoup(html, 'html.parser')
        lists = soup.select("#main > div > div")
        res1=[]
        for i in lists[2:]:
            link=i.select("a")[0]['href'][7:]
            link=link[:link.find("&sa=U")]
            link = unquote(link)
            try:
                title=i.select("h3 > div")[0].text.strip()
                company=i.select("a > div")[0].text.strip()
                date=i.select("div > div > div > div > span")[0].text.strip()
                n=int(re.findall("[0-9]+", date)[0])
                if "일" in date: date=datetime.now()-timedelta(days=n)
                elif "주" in date: date=datetime.now()-timedelta(weeks=n)
                elif "시간" in date: date=datetime.now()-timedelta(hours=n)
                elif "개월" in date: date=datetime.now()-timedelta(days=n*30)
                else: date=datetime.now()-timedelta(days=n*30)
                story=i.select("div> div > div > div > div > div")[0].text.strip()
                story=story[story.find(' 전 ·')+4:]
                res1.append([title, link, company, date, story])
            except:
                pass
        res1=df(res1, columns=['title', 'link', 'company', 'date', 'story'])
        res1=res1.sort_values(by='date', ascending=False).reset_index(drop=True)
        res1['assets']=code
    url2 = f'https://api-manager.upbit.com/api/v1/notices?page=1&per_page={days*10}'
    res2 = requests.get(url2)
    res2 = res2.json()
    res2 = df(res2['data']['list'])
    res2['link']=res2.id.apply(lambda x: f"https://api-manager.upbit.com/api/v1/notices/{x}")
    res2['assets']=res2.title.apply(lambda x: findAsset(x))
    res2=res2[['created_at','title','link','assets']]
    res2['story']=res2.title
    res2=res2.rename(columns={'created_at':'date'})
    res2['company']='업빗공지'
    res2.reset_index(drop=True)
    #업비트 공시 목록 읽기
    url3 = f'https://project-team.upbit.com/api/v1/disclosure?region=kr&per_page={days*10}'
    res3 = requests.get(url3)
    res3 = res3.json()
    res3 = df(res3['data']['posts'])
    res3 = res3[['start_date','text','url','assets']]
    res3 = res3.rename(columns={'start_date':'date', 'text':'title', 'url':'link'})
    res3['story']=res3.title
    res3['company']='상장사공지'
    res4 = pd.concat([res2,res3])
    res4.date = res4.date.apply(lambda x:  datetime.strptime(x.split("+")[0].replace('T',' '),"%Y-%m-%d %H:%M:%S"))
    if news:
        n = pd.concat([res1,res4]).reset_index(drop=True)
    else:
        n = res4
    n=n.loc[n.date>datetime.now()-timedelta(days=days)].sort_values('date', ascending=False).reset_index(drop=True)
    if sw: #KRW-BTC처럼 코드로 받을 경우 BTC만 남기기 위함임
        n = n.loc[n.assets==code.upper()].reset_index(drop=True) #코드가 있으면 줄인다..
    return n

#공지내 해당 코인을 찾아줌
def findAsset(t):
    codes_info['code']=codes_info.market.apply(lambda x:x[4:].lower())
    t=t.lower()
    res=''
    for i in range(codes_info.shape[0]):
        for j in ['code','korean_name','english_name']:
            if codes_info[j].iloc[i] in t:
                if codes_info['code'].iloc[i].upper() not in res:
                    if res!='':
                        res = res+", "+codes_info['code'].iloc[i].upper()
                    else:
                        res = codes_info['code'].iloc[i].upper()
    return res

class MyApp(QMainWindow):
    def __init__(self):
        super().__init__()
        self.size = QDesktopWidget().screenGeometry() #전체 화면 크기
        # print(self.size.width(),self.size.height())
        self.islogos = os.path.exists("logo/") #로고를 다운 받았는지?
        self.initUI()
        self.timer = QTimer(self)
        self.timer.setInterval(refresh_time*1000) #5초마다 새로고침
        self.timer.timeout.connect(self.addTable)
        self.timer.start()
        # self.worker.close()
        # self.tableClicked()
    def initUI(self):
        global intv, count, refresh_time
        self.setWindowIcon(QIcon('2bp_logo_v2.ico'))
        b = QLabel(self)
        b.setText("intv(mins):")
        b2 = QLabel(self)
        b2.setText("count:")
        b3 = QLabel(self)
        b3.setText("Refresh(secs):")
        cb = QComboBox(self)
        cb2 = QComboBox(self)
        cb3 = QComboBox(self)
        for i in [1,3, 5, 10, 15, 30, 60, 240, 1440, 1440*7, 1440*30, 1440*365]:
            cb.addItem(str(i))
        for i in range(10):
            cb2.addItem(str((i+1)*100))
        for i in range(50):
            cb3.addItem(str((i+1)*5))
        cb.setCurrentText(str(intv))
        cb2.setCurrentText(str(count))
        cb3.setCurrentText(str(refresh_time))

        cb.activated[str].connect(self.onActivated)
        cb2.activated[str].connect(self.onActivated2)
        cb2.activated[str].connect(self.onActivated3)
        self.tableWidget = QTableWidget()
        self.addTable()
        self.tableWidget.clicked.connect(self.tableClicked)

        # self.tableWidget.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        self.tableWidget.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeToContents)
        self.tableWidget.setSortingEnabled(True) #소팅 가능하게 변경
        self.tableWidget.setAlternatingRowColors(True) # 행마다 색깔을 변경 하는 코드
        self.gui_palette = QPalette() #반복되는 행의 색깔을 지정하는 코드
        self.gui_palette.setColor(QPalette.AlternateBase, QColor(0,0,0,0)) #반복되는 행의 색깔을 지정하는 코드

        wid = QWidget(self)
        self.setCentralWidget(wid)
        self.setGeometry(0, 0, int(self.size.width()*0.3), self.size.height()-100)
        layout = QVBoxLayout()
        layout1 = QHBoxLayout()
        layout2 = QHBoxLayout()
        layout1.addWidget(b)
        layout1.addWidget(cb)
        layout1.addWidget(b2)
        layout1.addWidget(cb2)
        layout1.addWidget(b3)
        layout1.addWidget(cb3)
        layout2.addWidget(self.tableWidget)
        layout.addLayout(layout1)
        layout.addLayout(layout2)
        wid.setLayout(layout)
        self.setWindowTitle('2bp V.0.5 - Upbit 현황판 무료 체험 버전')
        wid.show()
    def onActivated(self, text):
        global intv
        intv = int(text)

    def onActivated2(self, text):
        global count
        count = int(text)

    def onActivated3(self, text):
        global refresh_time
        refresh_time = int(text)

    def addTable(self):
        df=getSise(codes)
        self.tableWidget.clear()
        self.tableWidget.setRowCount(len(df.index))
        self.tableWidget.setColumnCount(len(df.columns)+1)
        # print(df.columns)
        self.tableWidget.setHorizontalHeaderLabels(df.columns.insert(0,'icon'))
        self.tableWidget.verticalHeader().setDefaultSectionSize(5)
#         self.tableWidget.setVerticalHeaderLabels(df.index)

        for i in tqdm(range(len(df.index))):
            for j in range(len(df.columns)+1):
                if j==0:
                    img = QImage()
                    path=f"logo/{df.iloc[i,0][4:]}.png"
                    if self.islogos and os.path.exists(path):
                        img.load(path)
                        _img = QPixmap.fromImage(img)
                    else:
                        try: os.mkdir('logo')
                        except: pass
                        url=f"https://static.upbit.com/logos/{df.iloc[i,0][4:]}.png"
                        r = requests.get(url,stream=True)
                        assert r.status_code == 200
                        assert img.loadFromData(r.content)
                        _img = QPixmap.fromImage(img).scaled(25, 25, Qt.KeepAspectRatio)
                        _img.save(f"logo/{df.iloc[i,0][4:]}.png")
                    w = QLabel()
                    w.setPixmap(_img)
                    self.tableWidget.setCellWidget(i,j,w)
                    continue
                value = df.iloc[i,j-1]
                item = QTableWidgetItem(isnum(value))
                self.tableWidget.setItem(i,j, item)
                if float(df.iloc[i,3])>0 and j>1: self.tableWidget.item(i, j).setForeground(QBrush(QColor(255,0,0)))
                elif j>1: self.tableWidget.item(i, j).setForeground(QBrush(QColor(0,0,255)))

    def refreshData(self):
        self.tableWidget.clear()
        df=getSise(codes)
        self.tableWidget.setHorizontalHeaderLabels(df.columns.insert(0,'icon'))
        for i in range(len(df.index)):
            for j in range(len(df.columns)+1):
                if j==0:
                    path=f"logo/{df.iloc[i,0][4:]}.png"
                    img = QImage()
                    if os.path.exists(path): #로고 파일을 한번만 다운 받으면 더이상 않받음
                        img.load(path)
                        _img = QPixmap.fromImage(img)
                    else:
                        break
                        url=f"https://static.upbit.com/logos/{df.iloc[i,0][4:]}.png"
                        r = requests.get(url,stream=True)
                        assert r.status_code == 200
                        assert img.loadFromData(r.content)
                        _img = QPixmap.fromImage(img).scaled(25, 25, Qt.KeepAspectRatio)
                        _img.save(f"logo/{df.iloc[i,0][4:]}.png")
                    w = QLabel()
                    w.setPixmap(_img)
                    # w.setGeometry(0,0,5,5)
                    # w.resize(20,20)
                    self.tableWidget.setCellWidget(i,j,w)
                    continue
                value = df.iloc[i,j-1]
        #                 print(i,j,value)
                item = QTableWidgetItem(str(value))
                self.tableWidget.setItem(i,j, item)
                if float(df.iloc[i,3])>0 and j>1: self.tableWidget.item(i, j).setForeground(QBrush(QColor(255,0,0)))
                elif j>1: self.tableWidget.item(i, j).setForeground(QBrush(QColor(0,0,255)))

    def tableClicked(self):
        global intv, count, coin_name
        try:
            r = self.tableWidget.currentIndex().row()
            coin_name=self.tableWidget.item(r, 1).data(0)
        except:
            coin_name="KRW-BTC"
        try: self.w.close()
        except: pass
        self.w = Mychart()
        self.w.chart(coin_name, intv, count)
        self.w.setGeometry(int(self.size.width()*0.3), 50, int(self.size.width()*0.45), 600)
        self.w.show()

        self.w2 = Orderbook()
        self.w2.setGeometry(int(self.size.width()*0.75), 50, int(self.size.width()*0.25), 750)
        self.w2.show()

        self.w3 = News()
        self.w3.setGeometry(int(self.size.width()*0.3), 700, int(self.size.width()*0.45), self.size.height()-700)
        self.w3.show()

        self.w4 = MarketDepth()
        self.w4.setGeometry(int(self.size.width()*0.75), 850, int(self.size.width()*0.25), self.size.height()-950)
        self.w4.show()

        self.worker = Worker('detail',coin_name)
        self.worker.order.connect(self.w2.addRow)
        self.worker.order.connect(self.w4.addDepth)
        self.worker.trade.connect(self.w2.addPrice)
        self.worker.ticker.connect(self.w2.addTicker)
        self.worker.start()
        return

    def closeEvent(self, event):
        sys.exit()

class Mychart(QMainWindow):
    def __init__(self):
        global coin_name, intv, count, refresh_time
        super().__init__()
        self.timer = QTimer(self)
        self.timer.setInterval(refresh_time*1000) #5초마다 새로고침
        self.timer.timeout.connect(lambda: self.chart(coin_name,intv,count))
        self.timer.start()

    def chart(self, codes, intv, count):

        d = getData(codes, intv, count).set_index("Date")
        self.setWindowTitle(f"{codes} {intv}min")
        self.setWindowIcon(QIcon(f"logo/{coin_name[4:]}.png"))
        self.main_widget = QWidget()
        self.setCentralWidget(self.main_widget)
        kwargs = dict(type='candle',mav=(5,10,20,60),volume=True,figratio=(4,3))
                            #mav는 이평선
        mc = mpf.make_marketcolors(up='r',down='b',edge='in',
                                   wick={'up':'red','down':'blue'},
                                   volume='in',ohlc='black')
        s  = mpf.make_mpf_style(base_mpl_style='seaborn', marketcolors=mc)

        fig, axlist = mpf.plot(d,**kwargs,style=s,returnfig=True, tight_layout=True)
        vbox = QVBoxLayout(self.main_widget)
        canvas = FigureCanvas(fig)
        vbox.addWidget(canvas)
        canvas.draw()

    def chart1(self, codes, intv, count):
        df = getData(codes, intv, count)
        layout = go.Layout(
                              margin=go.layout.Margin(
                                    l=0, #left margin
                                    r=5, #right margin
                                    b=0, #bottom margin
                                    t=0, #top margin
                                )
                            )
        fig = go.Figure(data=[go.Candlestick(x=df['Date'],
                        open=df['Open'],
                        high=df['High'],
                        low=df['Low'],
                        close=df['Close'],
                        increasing_line_color= 'red',
                        decreasing_line_color= 'blue'
                        )],
                       layout = layout)

        raw_html = '<html><head><meta charset="utf-8" />'
        raw_html += '<body>'
        raw_html += plotly.offline.plot(fig, include_plotlyjs='cdn', output_type='div')
        raw_html += '</body></html>'

        view = QWebEngineView(self)

        view.setHtml(raw_html)
        view.show()
        self.setWindowTitle(f"{codes} {intv}min")

        self.setCentralWidget(view)

class Worker(QThread):
    order = pyqtSignal(df)
    trade = pyqtSignal(dict)
    ticker = pyqtSignal(dict)

    def __init__(self, types, c_codes):
        super().__init__()
        self.ws_url='wss://api.upbit.com/websocket/v1'
        self.unsubs="bye"
        self.type=types
        if self.type in ['sise']:
            self.subs='[{"ticket":"UNIQUE_TICKET"},{"type":"ticker","codes": '+f"{codes}"+',"isOnlySnapshot":"true"}]'
        elif self.type in ['detail']:
            self.subs='[{"ticket":"UNIQUE_TICKET"},{"type":"ticker","codes": ["'+c_codes+'"]},{"type":"orderbook","codes":["'+c_codes+'"]},{"type":"trade","codes":["'+c_codes+'"]}]'

    def run(self):
        self.ws = WebSocketApp(self.ws_url, on_message = self.on_message, on_error = self.on_error, on_close = self.on_close)
        # enableTrace(True)
        self.ws.on_open = self.on_open
        self.ws.run_forever()

    def on_message(self, ws, message):
        msg=json.loads(message)
        if self.type in ['sise']:
            data=[msg['code'],msg['trade_price'],f"{msg['signed_change_price']:.2f}",f"{msg['signed_change_rate']*100:.2f}",f"{msg['acc_trade_price_24h']//100000000:.2f}"]
            self.price.emit(data)
            return
        elif self.type in ['detail']:
            types = msg['type'];
            # print(types)
            if types=='orderbook':
                data=df(msg['orderbook_units'])
                # print(msg['orderbook_units'])
                self.order.emit(data)
            elif types=='trade':
                data=msg
                # print(data)
                self.trade.emit(data)
            elif types=='ticker':
                data=msg
                # print(data)
                self.ticker.emit(data)

    def on_error(self, ws, error):
        print (error)
        return

    def on_close(self,*args):
        print ("### closed ###")

    def on_open(self, ws):
        print("opened!!")
        self.ws.send(self.subs)

    def close(self):
        print("closed~~")
        self.ws.send(self.unsubs)
        self.ws.close()

class Orderbook(QWidget):
    def __init__(self):
        super(Orderbook, self).__init__()
        self.ordebooksize=10
        self.trade_array=[]
        # self.setGeometry(800, 200, 300, 640)
        self.tableWidget = QTableWidget(self)
        self.tableWidget.resize(500, 780)
        # self.tableWidget.setRowHeight(0, 10)

    def closeEvent(self, event):
        mywindow.worker.close()
        try: mywindow.worker.close()
        except: pass
        event.accept()

    @pyqtSlot(df)
    def addRow(self, d):
        self.setWindowTitle(f"{coin_name} Orderbook")
        self.setWindowIcon(QIcon(f"logo/{coin_name[4:]}.png"))
        # self.tableWidget.clear()
        self.tableWidget.setColumnCount(3)
        self.tableWidget.setRowCount(21)

        self.tableWidget.verticalHeader().setVisible(False)
        self.tableWidget.verticalHeader().setDefaultSectionSize(5)
        self.tableWidget.horizontalHeader().setVisible(False)

        #self.tableWidget.setAlternatingRowColors(True)
        self.tableWidget.setColumnWidth(0, int(self.tableWidget.width() * 0.4))
        self.tableWidget.setColumnWidth(1, int(self.tableWidget.width() * 0.2))
        self.tableWidget.setColumnWidth(2, int(self.tableWidget.width() * 0.4))

        # print("recieved", d.loc[0,'ask_price'],d.loc[0,'ask_size'])
        d=d[:self.ordebooksize]
        ask_sum=(d['ask_size']*d['ask_price']).sum()
        bid_sum=(d['bid_size']*d['bid_price']).sum()
        for i in range(self.ordebooksize):
            item1 = QTableWidgetItem(isnum(d.loc[i,'ask_price']))
            item2 = self.addBar(d.loc[i,'ask_size']*d.loc[i,'ask_price']/ask_sum*100, True)
            item3 = QTableWidgetItem(isnum(d.loc[i,'bid_price']))
            item4 = self.addBar(d.loc[i,'bid_size']*d.loc[i,'bid_price']/bid_sum*100, False)
            self.tableWidget.setItem(self.ordebooksize-1-i,1, item1)
            self.tableWidget.setCellWidget(self.ordebooksize-1-i,0, item2)
            self.tableWidget.setItem(self.ordebooksize+i,1, item3)
            self.tableWidget.setCellWidget(self.ordebooksize+i,2, item4)
            self.tableWidget.item(self.ordebooksize-1-i,1).setTextAlignment(Qt.AlignCenter)
            self.tableWidget.item(self.ordebooksize-1-i,1).setForeground(QBrush(QColor(255,0,0)))
            self.tableWidget.item(self.ordebooksize+i,1).setTextAlignment(Qt.AlignCenter)
            self.tableWidget.item(self.ordebooksize+i,1).setForeground(QBrush(QColor(0,0,255)))

    # method for widgets
    def addBar(self, quantity, rightAlign=True):
        widget = QWidget()
        layout = QVBoxLayout(widget)
        pbar = QProgressBar()
#             pbar.setFixedHeight(20)
        pbar.setInvertedAppearance(rightAlign)

        if rightAlign:
            pbar.setStyleSheet("""
                QProgressBar {background-color : rgba(0, 0, 0, 0%);border : 1}
                QProgressBar::Chunk {background-color : rgba(255, 0, 0, 20%);border : 1}
            """)
            pbar.setAlignment(Qt.AlignRight|Qt.AlignVCenter)
        else:
            pbar.setStyleSheet("""
                QProgressBar {background-color : rgba(0, 0, 0, 0%);border : 1}
                QProgressBar::Chunk {background-color : rgba(0, 0, 255, 20%);border : 1}
            """)
        # set data
        pbar.setRange(0, 100)
        pbar.setFormat(f"{quantity:.2f}%")
        pbar.setValue(int(quantity))

        layout.addWidget(pbar)
        layout.setAlignment(Qt.AlignVCenter)
        layout.setContentsMargins(0, 0, 0, 0)
        widget.setLayout(layout)
        return widget

    @pyqtSlot(dict)
    def addPrice(self, d):
        # add trade hihglight
        for i in range(10):
            if self.tableWidget.item(9-i, 1).text()==isnum(d['trade_price']):
                self.tableWidget.item(9-i, 1).setBackground(QColor(255, 204, 204))
                break
            elif self.tableWidget.item(10+i, 1).text()==isnum(d['trade_price']):
                self.tableWidget.item(10+i, 1).setBackground(QColor(255, 204, 204))
                break
        #add trade history
        self.trade_array.insert(0,(d['trade_price'], d['trade_volume'],d['ask_bid']))
        self.tableWidget.setItem(10,0,QTableWidgetItem("체결내역"))
        if len(self.trade_array)>9: self.trade_array.pop()
        for i in range(len(self.trade_array)):
            item2 = QTableWidgetItem(f"{self.trade_array[i][0]}   {self.trade_array[i][1]}")
            self.tableWidget.setItem(11+i,0,item2)
            if self.trade_array[i][2]=="BID":
                self.tableWidget.item(11+i,0).setForeground(QBrush(QColor(255,0,0)))
            else:
                self.tableWidget.item(11+i,0).setForeground(QBrush(QColor(0,0,255)))

    @pyqtSlot(dict)
    def addTicker(self, d):
        trade_price = d['trade_price']  #현재 시세
        acc_ask_volume = d['acc_ask_volume']  #금일 누적 매도
        acc_bid_volume = d['acc_bid_volume']  #금일 누적 매수
        opening_price = d['opening_price']  #금일 시작가
        self.inputCell(9,2,f"체결강도:{acc_bid_volume/acc_ask_volume*100:0.2f}%")  #체결강도
        self.inputCell(0,2,f"거래량:{d['acc_trade_volume_24h']:.2f}개")  #거래량
        self.inputCell(1,2,f"거래액:{int(d['acc_trade_price_24h']/100000000)}억")  #거래대금
        self.inputCell(2,2,f"52최고:{isnum(d['highest_52_week_price'])}")  #52주 최고
        self.inputCell(3,2,f"52일자:{d['highest_52_week_date']}")  #52주 최고일
        self.inputCell(4,2,f"52최저:{isnum(d['lowest_52_week_price'])}")  #52주 최저
        self.inputCell(5,2,f"52일자:{d['lowest_52_week_date']}")  #52주 최저일
        self.inputCell(6,2,f"전일종가:{d['prev_closing_price']}")  #전일종가
        self.inputCell(7,2,f"당일고가:{d['high_price']}")  #당일고가
        # self.inputCell(8,2,f"고가대비:{d['high_price']/opening_price*100-100:0.2}%")  #당일고가
        # self.inputCell(10,2,f"저가대비:{d['low_price']/opening_price*100-100:0.2}%")  #당일1가
        self.inputCell(8,2,f"당일저가:{d['low_price']}")  #당일저가

    def inputCell(self, i,j,value):
        item = QTableWidgetItem(value)
        self.tableWidget.setItem(i,j,item)

class News(QWidget):
    def __init__(self):
        super(News, self).__init__()
        # self.setGeometry(800, 200, 300, 640)
        self.tableWidget = QTableWidget(self)
        self.tableWidget.resize(int(mywindow.size.width()*0.45), 800)
        self.tableWidget.clicked.connect(self.tableClicked)
        self.setWindowTitle(f"{coin_name} News")
        self.setWindowIcon(QIcon(f"logo/{coin_name[4:]}.png"))
        self.news=getCoinNews(coin_name)
        t=self.news[['title', 'date']]
        if t.shape[0]>0:
            self.tableWidget.horizontalHeader().setSectionResizeMode(QHeaderView.ResizeToContents)
            self.tableWidget.setSortingEnabled(True) #소팅 가능하게 변경
            self.tableWidget.verticalHeader().setDefaultSectionSize(5)
            self.tableWidget.setAlternatingRowColors(True) # 행마다 색깔을 변경 하는 코드
            self.gui_palette = QPalette() #반복되는 행의 색깔을 지정하는 코드
            self.gui_palette.setColor(QPalette.AlternateBase, QColor(0,0,0,0)) #반복되는 행의 색깔을 지정하는 코드
            self.tableWidget.setRowCount(t.shape[0])
            self.tableWidget.setColumnCount(t.shape[1])
            self.tableWidget.setHorizontalHeaderLabels(t.columns)
            for i in range(t.shape[0]):
                for j in range(t.shape[1]):
                    value = t.iloc[i,j]
                    item = QTableWidgetItem(str(value))
                    self.tableWidget.setItem(i,j, item)

    def tableClicked(self):
        r = self.tableWidget.currentIndex().row()
        url=self.news.link.iloc[r]
        # print(url)
        webbrowser.open(url, new=2)

class MarketDepth(QMainWindow):
    def __init__(self):
        super().__init__()

    @pyqtSlot(df)
    def addDepth(self, d):
        width=d.ask_price.diff().mean()
        d['ask_cum']=d['ask_size'].cumsum()
        d['bid_cum']=d['bid_size'].cumsum()
        # d=d.fillna(method="ffill")
        ask=d[['ask_price', 'ask_cum']].sort_values('ask_price').set_index('ask_price')
        bid=d[['bid_price', 'bid_cum']].sort_values('bid_price').set_index('bid_price')
        d=pd.concat([bid,ask])
        self.setWindowTitle(f"{coin_name} Market Depth")
        self.setWindowIcon(QIcon(f"logo/{coin_name[4:]}.png"))
        self.main_widget = QWidget()
        self.setCentralWidget(self.main_widget)
        canvas = FigureCanvas(Figure())
        vbox = QVBoxLayout(self.main_widget)
        vbox.addWidget(canvas)
        self.ax = canvas.figure.subplots()
        self.ax.tight_layout=True
        # self.ax.clear()
        self.ax.bar(x=d.index, height=d.bid_cum.to_list(), width = width, color='red')
        self.ax.bar(x=d.index, height=d.ask_cum.to_list(), width = width, color='blue')
        self.show()


if __name__ == '__main__':
    codes_info=getCoinName()
    codes=codes_info['market'].to_list()
    intv=15
    count=100
    refresh_time=15
    coin_name=''

    sys.argv.append("--disable-web-security")
    app = QApplication(sys.argv)
    mywindow = MyApp()
    mywindow.show()
    app.exec_()
