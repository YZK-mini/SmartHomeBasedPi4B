package com.example.myapplication.ui.dashboard;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentDashboardBinding;
import com.example.myapplication.ui.PcmToWavUtil;
import com.example.myapplication.ui.SharedClass;
import static com.example.myapplication.ui.SharedClass.socketClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    public boolean tag = false;
    private AudioRecord mAudioRecord;
    public static final int SAMPLE_RATE_HZ = 16000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private boolean isRecording = false;
    int minBufferSize = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
    public String pcmPath;
    public PcmToWavUtil ptwUtil;
    public String wavFilePath;
    private textChange change;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ptwUtil = PcmToWavUtil.getInstance();

        pcmPath = requireContext().getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + "/Audio.pcm";
        wavFilePath = requireContext().getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + "/Audio.wav";

        change = new textChange();
        change.start();

        binding.imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!tag) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });

        return root;
    }

    @SuppressLint("MissingPermission")
    public void startRecord() {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
        final byte data[] = new byte[minBufferSize];
        final File fileAudio = new File(pcmPath);

        if (fileAudio.exists()) {
            fileAudio.delete();
        }

        mAudioRecord.startRecording();
        isRecording = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(fileAudio);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (null != fos) {
                    while (isRecording) {
                        int read = mAudioRecord.read(data, 0, minBufferSize);
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                fos.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        Toast.makeText(requireContext(), "开始录音", Toast.LENGTH_SHORT).show();
        tag = true;
    }

    public void stopRecord() {
        isRecording = false;
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        ptwUtil.pcmToWav(pcmPath, wavFilePath, true);
        Toast.makeText(requireContext(), "录音完毕", Toast.LENGTH_SHORT).show();
        tag = false;

        try {
            socketClient.sendAudioFile(requireContext());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "发送失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public class textChange extends Thread {
        private boolean running = true;

        public void terminate() {
            running = false;
        }

        @Override
        public void run() {
            try {
                while (running) {
                    TimeUnit.SECONDS.sleep(1);
                    if (binding != null) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (binding.recText.getText() != SharedClass.newRecText) {
                                    binding.recText.setText(SharedClass.newRecText);
                                }
                                if (binding.returnText.getText() != SharedClass.newReturnText) {
                                    binding.returnText.setText(SharedClass.newReturnText);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("出错");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (change != null) {
            change.terminate();
            change = null;
        }
        binding = null;
    }
}
