//package com.example.myapplication.ui;
//
//import java.io.*;
//import java.net.*;
//
//public class SocketClient {
//    private Socket socket;
//    private InputStream input;
//    private DataOutputStream output;
//
//    public void connect(String serverAddress, int port) {
//        try {
//            socket = new Socket(serverAddress, port);
//            input = socket.getInputStream();
//            output = new DataOutputStream(socket.getOutputStream());
//            // 连接成功，可以开始通信
//        } catch (UnknownHostException e) {
//            System.err.println("无法找到主机: " + serverAddress);
//        } catch (IOException e) {
//            System.err.println("无法打开连接到: " + serverAddress + " 的套接字");
//        }
//    }
//
//    public void disconnect() {
//        try {
//            if (input != null) {
//                input.close();
//            }
//            if (output != null) {
//                output.close();
//            }
//            if (socket != null) {
//                socket.close();
//            }
//        } catch (IOException e) {
//            System.err.println("断开连接时发生错误: " + e.getMessage());
//        }
//    }
//
//    public void sendMessage(String message) {
//        try {
//            output.writeUTF(message);
//            output.flush();
//        } catch (IOException e) {
//            System.err.println("发送消息时发生错误: " + e.getMessage());
//        }
//    }
//
//    public String receiveMessage() {
//        String message = null;
//        try {
//            final byte[] ReadBuffer = new byte[2048];
//            input.read(ReadBuffer);
//            System.out.println(ReadBuffer);
//        } catch (IOException e) {
//            System.err.println("接收消息时发生错误: " + e.getMessage());
//        }
//        return message;
//    }
//}
package com.example.myapplication.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.myapplication.R;
import com.example.myapplication.ui.SharedClass;
import com.example.myapplication.ui.home.HomeFragment;

public class SocketClient {
    private static final Logger LOGGER = Logger.getLogger(SocketClient.class.getName());

    private Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;
    private Monitor receiver;
    public int times = 0;
    public String[] strArray;

    // 构造函数：初始化客户端并连接到服务器
    public SocketClient(String serverIp, int serverPort) throws IOException {
        clientSocket = new Socket(serverIp, serverPort);
        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());
        receiver = new Monitor(clientSocket);
        receiver.start();
    }

    // 发送控制消息
    public void sendControlMessage(int tag, Object info) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    changeMode(0);
                    TimeUnit.MILLISECONDS.sleep(10);
                    JSONObject message = new JSONObject();
                    message.put("tag", tag);
                    message.put("info", info);
                    sendMessage(message.toString());
                    LOGGER.info("消息发送成功");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "发送控制消息时出错: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    // 发送音频文件
    public void sendAudioFile(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String wavFilePath = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + "/Audio.wav";
                File audioFile = new File(wavFilePath);

                // 检查文件是否存在
                if (!audioFile.exists()) {
                    LOGGER.severe("音频文件不存在: " + wavFilePath);
                    return;
                }

                try (FileInputStream is = new FileInputStream(audioFile)) {
                    changeMode(1);
                    byte[] buffer = new byte[2048];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        TimeUnit.MILLISECONDS.sleep(10);
                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }
                    LOGGER.info(" 发送成功");
                    out.write("EOF".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "发送音频文件时出错: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    // 切换模式
    public void changeMode(int mode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject message = new JSONObject();
                    message.put("tag", 0);
                    message.put("info", mode);
                    sendMessage(message.toString());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "切换模式时出错: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    // 关闭连接
    public void closeConnection() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "关闭连接时出错: " + e.getMessage(), e);
        }
    }

    // 发送消息
    private void sendMessage(String msg) throws IOException {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + msgBytes.length);
        byteBuffer.putInt(msgBytes.length);
        byteBuffer.put(msgBytes);
        out.write(byteBuffer.array());
        out.flush();
    }

    // 内部类：用于监听和接收服务器消息
    public class Monitor extends Thread {
        private DataInputStream in;

        public Monitor(Socket clientSocket) throws IOException {
            this.in = new DataInputStream(clientSocket.getInputStream());
        }

        @Override
        public void run() {
            try {
                System.out.println("接收进程已启动");
                while (true) {
                    String receivedInfo = receiveMessage();
                    if (receivedInfo != null) {
                        JSONObject tempInfo = new JSONObject(receivedInfo);
                        int tag = tempInfo.getInt("tag");
                        if (tag == 6) {
                            receivePicture();
                            SharedClass.receivedPicture = 1;
                        } else if (tag == 5){
                            receiveEnvironment(tempInfo);
                        } else if (tag == 8){
                            String[] temp = tempInfo.getString("info").split("[|]");
                            SharedClass.newRecText = temp[0];
                            SharedClass.newReturnText = temp[1];
                        } else if (tag == 7) {
                            SharedClass.receivedFireWarn = 1;
                        } else if (tag == 1) {
                            SharedClass.light1 = tempInfo.getInt("info");
                            System.out.println(1);
                        } else if (tag == 2) {
                            SharedClass.light2 = tempInfo.getInt("info");
                            System.out.println(2);
                        } else if (tag == 3) {
                            SharedClass.light3 = tempInfo.getInt("info");
                            System.out.println(3);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "接收数据时出错: " + e.getMessage(), e);
            }
        }

        // 接收消息
        private String receiveMessage() throws IOException {
            int length = in.readInt();
            byte[] msgBytes = new byte[length];
            in.readFully(msgBytes);
            return new String(msgBytes, StandardCharsets.UTF_8);
        }

        // 接收图片
        private void receivePicture() {
            try {
                File publicStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File pictureFile = new File(publicStorageDir, "capture.jpg");

                FileOutputStream fos = new FileOutputStream(pictureFile);
                byte[] buffer = new byte[2048];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    String dataStr = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    if (dataStr.contains("EOF")) {
                        fos.write(buffer, 0, bytesRead - 3); // 移除 "EOF"
                        break;
                    }
                    fos.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "接收图片时出错: " + e.getMessage(), e);
            }
        }

        // 处理接收到的消息
        private void receiveEnvironment(JSONObject info) throws JSONException {
            strArray=info.getString("info").split("[|]");
            SharedClass.temperature = strArray[0];
            SharedClass.humidity = strArray[1];
            SharedClass.weather = strArray[2];
            SharedClass.tem = strArray[3];
        }
    }
}
