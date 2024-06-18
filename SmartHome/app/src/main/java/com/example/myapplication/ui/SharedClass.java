package com.example.myapplication.ui;

import android.app.Application;
public class SharedClass extends Application{
    public static SocketClient socketClient;

    public static String newRecText = "-";
    public static String newReturnText = "-";

    public static int light1 = 0;
    public static int light2 = 0;
    public static int light3 = 0;

    public static int receivedPicture = 0;
    public static int receivedFireWarn = 0;

    public static String temperature = "-";
    public static String humidity = "-";
    public static String weather = "-";
    public static String tem = "-";
}
