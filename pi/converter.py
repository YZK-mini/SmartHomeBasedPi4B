import requests
import json
import base64


def audio2text() -> str:
    with open("Audio.wav", "rb") as f:
        data = f.read()
    url = "https://vop.baidu.com/server_api"
    payload = json.dumps({
        "format": "wav",
        "rate": 16000,
        "channel": 1,
        "cuid": "CAeiCI1yZWLko1UK8tXjXHzkdqpvBzBt",
        "token": getAccessToken(),
        "speech": base64.b64encode(data).decode('utf-8'),
        "len": len(data)
    })
    headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    }
    response = requests.request("POST", url, headers=headers, data=payload)
    print(response.json())
    print(response.json()['result'][0])
    return response.json()['result'][0]


def getAccessToken() -> str:
    API_KEY = "cnYSauVDwle4K7UruF0E6wrA"
    SECRET_KEY = "9mJmVP8ttMJuPQDwx3xBGuZRBMHq3UR9"
    url = "https://aip.baidubce.com/oauth/2.0/token"
    params = {"grant_type": "client_credentials", "client_id": API_KEY, "client_secret": SECRET_KEY}
    return str(requests.post(url, params=params).json().get("access_token"))
