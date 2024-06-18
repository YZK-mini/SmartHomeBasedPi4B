import json
import socketserver
import struct
import time
import threading

from device import Device
from converter import audio2text
from paraphrasing import semanticAnalysis


class Mess:
    def __init__(self, tag, info):
        self.tag = tag
        self.info = info

    def toDict(self):
        return {'tag': self.tag, 'info': self.info}

    @staticmethod
    def fromDict(d):
        return Mess(d['tag'], d['info'])


class PiServer(socketserver.BaseRequestHandler):
    def handle(self):
        runMode = 0

        devCheck = DeviceMonitor(self.request)
        devCheck.start()

        while True:
            try:
                recvInf = self.recvMsg()
                if recvInf:
                    tempInf = Mess.fromDict(json.loads(recvInf))
                    print(tempInf)
                    if tempInf.tag == 0:
                        runMode = tempInf.info
                    if runMode == 0:
                        controlInf = Mess.fromDict(json.loads(self.recvMsg()))
                        self.ctlDevice(controlInf.tag, controlInf.info)
                    elif runMode == 1:
                        controlList = self.recvAudio()
                        for control in controlList:
                            self.ctlDevice(control[0], control[1])
            except ConnectionResetError:
                devCheck._running = False

    def recvMsg(self):
        rawMsgLen = self.request.recv(4)
        if not rawMsgLen:
            return None
        msglen = struct.unpack('>I', rawMsgLen)[0]
        return self.request.recv(msglen).decode('utf-8')

    def sendMsg(self, msg):
        msg = msg.encode('utf-8')
        msg = struct.pack('>I', len(msg)) + msg
        self.request.sendall(msg)

    def recvAudio(self) -> list:
        with open('Audio.wav', 'wb') as f:
            while True:
                time.sleep(0.01)
                data = self.request.recv(2048)
                if b'EOF' in data:
                    data = data.replace(b'EOF', b'')
                    f.write(data)
                    break
                f.write(data)
        text = audio2text()
        deviceList, content = semanticAnalysis(text)
        self.sendMess(8, text + "|" + content)
        return deviceList

    def sendMess(self, tag, info):
        mess = Mess(tag, info)
        self.sendMsg(json.dumps(mess.toDict()))

    def ctlDevice(self, tag, movement):
        if tag == 1:
            dev.bedroomLed(movement)
            self.sendMess(1, movement)
        elif tag == 2:
            dev.livingRoomLed(movement)
            self.sendMess(2, movement)
        elif tag == 3:
            dev.kitchenLed(movement)
            self.sendMess(3, movement)
        elif tag == 4:
            dev.curtainCtl(movement)
        elif tag == 5:
            T, H = dev.getTH()
            weather, temperature = dev.getWeather()
            tempStr = f"{T}|{H}|{weather}|{temperature}"
            self.sendMess(5, tempStr)
            print(tempStr)
        elif tag == 6:
            dev.doorCtl(movement)


class DeviceMonitor(threading.Thread):
    def __init__(self, request):
        super().__init__()
        self.request = request

    def run(self):
        btnState = dev.knockChecked()
        lightState = dev.photoSensitive()
        fireState = dev.fireDetect()
        smokeState = dev.smokeDetect()
        while True:
            knocked = dev.knockChecked()
            if not knocked:
                if btnState != knocked:
                    dev.capture()
                    time.sleep(2)
                    self.sendPicture()
            btnState = knocked

            light = dev.photoSensitive()
            temp = (lightState != light) and (light != dev.curtainState)
            dev.curtainCtl(temp)
            lightState = light

            fire = dev.fireDetect()
            smoke = dev.smokeDetect()
            if not fire or not smoke:
                if fire != fireState or smoke != smokeState:
                    mess = Mess(7, 1)
                    print("火灾发生")
                    self.sendMsg(json.dumps(mess.toDict()))

    def sendPicture(self):
        mess = Mess(6, 0)
        self.sendMsg(json.dumps(mess.toDict()))
        with open("capture.jpg", 'rb') as f:
            print("Sending to client...")
            data = f.read(2048)
            while data:
                time.sleep(0.01)
                self.request.send(data)
                data = f.read(2048)
        time.sleep(0.01)
        self.request.send(b'EOF')
        print("Sent successfully.")

    def sendMsg(self, msg):
        msg = msg.encode('utf-8')
        msg = struct.pack('>I', len(msg)) + msg
        self.request.sendall(msg)


def main():
    global dev
    dev = Device()
    serverIp = ('192.168.137.215', 8000)
    server = socketserver.ThreadingTCPServer(serverIp, PiServer)
    try:
        print("Server started...")
        server.serve_forever()
    except KeyboardInterrupt:
        print("Server is shutting down...")
        server.shutdown()
        server.server_close()


if __name__ == '__main__':
    main()
