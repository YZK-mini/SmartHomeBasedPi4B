package com.example.myapplication.ui.home;

import static com.example.myapplication.ui.SharedClass.socketClient;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.ui.SharedClass;
import com.example.myapplication.ui.SocketClient;
import com.example.myapplication.ui.dashboard.DashboardFragment;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private checkedChange change;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.light1.setChecked(SharedClass.light1 == 1);
        binding.light2.setChecked(SharedClass.light2 == 1);
        binding.light3.setChecked(SharedClass.light3 == 1);

        binding.connectdev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //连接服务器
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            socketClient = new SocketClient("192.168.137.215", 8000);
                            System.out.println("Connected");
                            change = new checkedChange();
                            change.start();
                            socketClient.sendControlMessage(5, 1);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });

        binding.curtain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开窗帘
                socketClient.sendControlMessage(4, 1);
            }
        });

        binding.door.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开门
                socketClient.sendControlMessage(6, 1);
            }
        });

        binding.temprec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.temptext.setText(SharedClass.temperature + "°C");
                binding.humiditytext.setText(SharedClass.humidity + "%");
                binding.othertext.setText(SharedClass.weather+"\n"+SharedClass.tem+"°C");
            }
        });

        binding.humidityrec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.temptext.setText(SharedClass.temperature + "°C");
                binding.humiditytext.setText(SharedClass.humidity + "%");
                binding.othertext.setText(SharedClass.weather+"\n"+SharedClass.tem+"°C");
                handlePictureChange();
            }
        });

        binding.weatherrec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.temptext.setText(SharedClass.temperature + "°C");
                binding.humiditytext.setText(SharedClass.humidity + "%");
                binding.othertext.setText(SharedClass.weather+"\n"+SharedClass.tem+"°C");
                handleFireWarnChange();
            }
        });

        binding.light1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.light1.isChecked()){
                    socketClient.sendControlMessage(1 , 1);
                }
                else{
                    socketClient.sendControlMessage(1 , 0);
                }
            }
        });

        binding.light2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.light2.isChecked()){
                    socketClient.sendControlMessage(2 , 1);
                }
                else{
                    socketClient.sendControlMessage(2 , 0);
                }
            }
        });

        binding.light3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.light3.isChecked()){
                    socketClient.sendControlMessage(3 , 1);
                }
                else{
                    socketClient.sendControlMessage(3 , 0);
                }
            }
        });

        return root;
    }

    public void showPictureAlertDialog() {
        ImageView img = new ImageView(requireContext());

        File publicStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File pictureFile = new File(publicStorageDir, "capture.jpg");

        if (pictureFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
            img.setImageBitmap(bitmap);
        }

        // 创建AlertDialog.Builder实例
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // 设置对话框标题
        builder.setTitle("请注意");
        // 设置对话框消息
        builder.setMessage("是否开门");
        builder.setView(img);

        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                socketClient.sendControlMessage(6, 1);
                SharedClass.receivedPicture = 0;
            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SharedClass.receivedPicture = 0;
            }
        });

        // 创建并显示对话框
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void showFireAlertDialog() {
        ImageView img = new ImageView(requireContext());

        img.setImageResource(R.drawable.firewarning);

        // 创建AlertDialog.Builder实例
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // 设置对话框标题
        builder.setTitle("警告");
        // 设置对话框消息
        builder.setMessage("有火焰与烟雾报警");
        builder.setView(img);

        builder.setNegativeButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // 创建并显示对话框
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public class checkedChange extends Thread {
        @Override
        public void run() {
            try {
                while (true){
                    TimeUnit.SECONDS.sleep(1);
                    if (SharedClass.receivedFireWarn == 1){
                        handleFireWarnChange();
                        SharedClass.receivedFireWarn = 0;
                    }
                    if (SharedClass.receivedPicture == 1){
                        handlePictureChange();
                        SharedClass.receivedPicture = 0;
                    }
                }
            } catch (Exception e) {
                System.out.println("出错");
            }
        }
    }

    private void handlePictureChange() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showPictureAlertDialog();
            }
        });
    }

    private void handleFireWarnChange() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showFireAlertDialog();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}