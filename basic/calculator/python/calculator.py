import sys
from PyQt5 import QtWidgets
from PyQt5 import QtGui
from PyQt5 import uic
from PyQt5 import QtCore
from PyQt5.QtCore import pyqtSlot


class Form(QtWidgets.QDialog):
    def __init__(self, parent=None):
        QtWidgets.QDialog.__init__(self, parent)
        self.ui = uic.loadUi("calculator.ui", self)
        self.ui.show()

    # def slot0(self):
    #     self.ui.label.setText("0")
    #
    def slot1(self):
        string = self.ui.label.text()+"1"
        self.ui.label.setText(string)

    def slot2(self):
        string = self.ui.label.text()+"2"
        self.ui.label.setText(string)

    def slot3(self):
        string = self.ui.label.text()+"3"
        self.ui.label.setText(string)

    def slot4(self):
        string = self.ui.label.text()+"4"
        self.ui.label.setText(string)

    def slot5(self):
        string = self.ui.label.text()+"5"
        self.ui.label.setText(string)

    def slot6(self):
        string = self.ui.label.text()+"6"
        self.ui.label.setText(string)

    def slot7(self):
        string = self.ui.label.text()+"7"
        self.ui.label.setText(string)

    def slot8(self):
        string = self.ui.label.text()+"8"
        self.ui.label.setText(string)

    def slot9(self):
        string = self.ui.label.text()+"9"
        self.ui.label.setText(string)

    def slot0(self):
        string = self.ui.label.text()+"0"
        self.ui.label.setText(string)

    def slot_dot(self):
        string = self.ui.label.text()+"."
        self.ui.label.setText(string)

    def slotadd(self):
        string = self.ui.label.text()+"+"
        self.ui.label.setText(string)

    def slotssub(self):
        string = self.ui.label.text()+"-"
        self.ui.label.setText(string)

    def slotmul(self):
        string = self.ui.label.text()+"*"
        self.ui.label.setText(string)

    def slotdiv(self):
        string = self.ui.label.text()+"/"
        self.ui.label.setText(string)

    def slot_reset(self):
        self.ui.label.setText("")

    def slot_left_cap(self):
        string = self.ui.label.text()+"("
        self.ui.label.setText(string)

    def slot_right_cap(self):
        string = self.ui.label.text()+")"
        self.ui.label.setText(string)

    def slot_back_space(self):
        string = self.ui.label.text()
        newstring=string[:-1]
        self.ui.label.setText(newstring)

    def slot_evaluate(self):
        try:
            value= str(eval(self.ui.label.text()))
            self.ui.label.setText(value)
        except SyntaxError:
            self.ui.label.setText("")

    def slot_BG_transparent(self):
        value=1-self.ui.horizontalSlider.value()/10
        self.ui.label.setText(str(value))
        self.setWindowOpacity(value)


if __name__ == '__main__':
    app = QtWidgets.QApplication(sys.argv)
    w = Form()
    exp = ""
    sys.exit(app.exec())
