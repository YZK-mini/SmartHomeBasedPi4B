package com.example.myapplication.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.ui.SocketClient;

public class HomeViewModel extends ViewModel {
    public int humidity = 0;
    public int temperature = 0;
    public String ipaddress = "";
    public String answer = "";
}