from PyQt4.QtGui import *
from PyQt4.QtCore import *
import sys

from PyQt4 import QtCore, QtGui, uic

# For opening links
import webbrowser


#Clickable QLabel
class ExtendedQLabel(QLabel):
#class ExtendedQLabel(QtGui.QLabel):

    #def __init__(self):
    def __init(self, parent):
	#super(ExtendedQLabel, self).__init__()
        QLabel.__init__(self, parent)

        #Set color of labels
        self.palette = QtGui.QPalette()
	self.setPalette(self.palette)
        self.palette.setColor(QtGui.QPalette.Foreground, QtCore.Qt.darkGreen)

	self.setMouseTracking(True)
	#Set tooltip functionality
	self.QToolTip.showText("hello2")	
	#self.setToolTip('')
	#self.

    def mouseReleaseEvent(self, ev):
        self.emit(SIGNAL('clicked()'))

#    def mouse_over_event(self, ev):
#	self.emit(SIGNAL('Mouse_over()'))

    def open_url(self):
	dumstr = str(self.text())
	webbrowser.open(dumstr)