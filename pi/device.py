import adafruit_dht
import RPi.GPIO as GPIO
import board
import time
import requests
from picamera2 import Picamera2


class Device:
    def __init__(self) -> None:
        self.camera = Picamera2()
        self.camera.still_configuration.size = (1024, 786)
        self.btnPin = 26
        self.ledBedroomPin = 18
        self.ledLivingroomPin = 15
        self.ledKitchenPin = 23
        self.photoSensitivePin = 24
        self.dhtPin = board.D4
        self.firePin = 7
        self.smokePin = 1
        self.dhtDevice = adafruit_dht.DHT22(pin=self.dhtPin)
        self.sgCtl = SgController()
        self.curtainState = 0
        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)
        GPIO.setup(self.btnPin, GPIO.IN)
        GPIO.setup(self.ledBedroomPin, GPIO.OUT, initial=GPIO.LOW)
        GPIO.setup(self.ledKitchenPin, GPIO.OUT, initial=GPIO.LOW)
        GPIO.setup(self.ledLivingroomPin, GPIO.OUT, initial=GPIO.LOW)
        GPIO.setup(self.photoSensitivePin, GPIO.IN)
        GPIO.setup(self.firePin, GPIO.IN)
        GPIO.setup(self.smokePin, GPIO.IN)

    def bedroomLed(self, state: int):
        if state == 1:
            GPIO.output(self.ledBedroomPin, GPIO.HIGH)
        elif state == 0:
            GPIO.output(self.ledBedroomPin, GPIO.LOW)
        else:
            print("未识别命令")

    def livingRoomLed(self, state: int):
        if state == 1:
            GPIO.output(self.ledLivingroomPin, GPIO.HIGH)
        elif state == 0:
            GPIO.output(self.ledLivingroomPin, GPIO.LOW)
        else:
            print("未识别命令")

    def kitchenLed(self, state: int):
        if state == 1:
            GPIO.output(self.ledKitchenPin, GPIO.HIGH)
        elif state == 0:
            GPIO.output(self.ledKitchenPin, GPIO.LOW)
        else:
            print("未识别命令")

    def getTH(self):
        time.sleep(0.5)
        while True:
            try:
                T = self.dhtDevice.temperature
                H = self.dhtDevice.humidity
                if T is not None and H is not None:
                    return T, H
            except RuntimeError as err:
                print(err.args[0])
                time.sleep(0.5)
                continue

    def getWeather(self):
        url = 'https://restapi.amap.com/v3/weather/weatherInfo?city=******&key=***'
        res = requests.get(url)
        data = res.json()['lives'][0]
        return data['weather'], data['temperature']

    def photoSensitive(self):
        return GPIO.input(self.photoSensitivePin)

    def fireDetect(self):
        return GPIO.input(self.firePin)

    def smokeDetect(self):
        return GPIO.input(self.smokePin)

    def capture(self):
        self.camera.start_and_capture_file("capture.jpg")
        self.camera.stop_preview()

    def knockChecked(self):
        return GPIO.input(self.btnPin)

    def curtainCtl(self, state: int):
        if state == 1:
            if self.curtainState == 0:
                self.sgCtl.sgOpen()
                self.curtainState = 1
            elif self.curtainState == 1:
                self.sgCtl.sgClose()
                self.curtainState = 0

    def doorCtl(self, state: int):
        if state == 1:
            self.sgCtl.sgN90()
            time.sleep(3)
            self.sgCtl.sgP90()


class SgController:
    def __init__(self) -> None:
        self.sg90Pin = 12
        GPIO.setup(self.sg90Pin, GPIO.OUT, initial=GPIO.LOW)
        self.pwm = GPIO.PWM(self.sg90Pin, 50)
        self.pwm.start(0)

    def sgN90(self):
        self.pwm.ChangeDutyCycle(2.5)
        time.sleep(0.1)
        self.pwm.ChangeDutyCycle(0)

    def sgP90(self):
        self.pwm.ChangeDutyCycle(12.5)
        time.sleep(0.1)
        self.pwm.ChangeDutyCycle(0)

    def sgOpen(self):
        self.pwm.ChangeDutyCycle(2.5)
        time.sleep(1)
        self.pwm.ChangeDutyCycle(0)

    def sgClose(self):
        self.pwm.ChangeDutyCycle(12.5)
        time.sleep(1)
        self.pwm.ChangeDutyCycle(0)

    def stop(self):
        self.pwm.stop()


if __name__ == '__main__':
    dev = Device()
    print(dev.getTH())
