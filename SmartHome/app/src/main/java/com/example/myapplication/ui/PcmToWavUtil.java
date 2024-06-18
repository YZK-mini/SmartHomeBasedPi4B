package com.example.myapplication.ui;

import android.media.AudioFormat;
import android.media.AudioRecord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcmToWavUtil {
    private int mBufferSize; // 缓存的音频大小
    private int mSampleRate = 16000; // 采样率
    private int mChannel = AudioFormat.CHANNEL_IN_MONO; // 单声道
    private int mEncoding = AudioFormat.ENCODING_PCM_16BIT; // 16位PCM

    private static class SingleHolder {
        static PcmToWavUtil mInstance = new PcmToWavUtil();
    }

    public static PcmToWavUtil getInstance() {
        return SingleHolder.mInstance;
    }

    public PcmToWavUtil() {
        this.mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannel, mEncoding);
    }

    public PcmToWavUtil(int sampleRate, int channel, int encoding) {
        this.mSampleRate = sampleRate;
        this.mChannel = channel;
        this.mEncoding = encoding;
        this.mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannel, mEncoding);
    }

    /**
     * pcm文件转wav文件
     *
     * @param inFilename  源文件路径
     * @param outFilename 目标文件路径
     * @param deleteOrg   是否删除源文件
     */
    public void pcmToWav(String inFilename, String outFilename, boolean deleteOrg) {
        FileInputStream in = null;
        FileOutputStream out = null;
        BufferedInputStream bufferedIn = null;
        BufferedOutputStream bufferedOut = null;
        try {
            long totalAudioLen;
            long totalDataLen;
            long longSampleRate = mSampleRate;
            int channels = 1; // 单声道
            long byteRate = 16 * mSampleRate * channels / 8; // 每秒的字节数

            byte[] data = new byte[mBufferSize];

            in = new FileInputStream(inFilename);
            bufferedIn = new BufferedInputStream(in);
            out = new FileOutputStream(outFilename);
            bufferedOut = new BufferedOutputStream(out);

            totalAudioLen = bufferedIn.available();
            totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(bufferedOut, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            int bytesRead;
            while ((bytesRead = bufferedIn.read(data)) != -1) {
                bufferedOut.write(data, 0, bytesRead);
            }

            bufferedOut.flush();
            if (deleteOrg) {
                new File(inFilename).delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedIn != null) {
                    bufferedIn.close();
                }
                if (bufferedOut != null) {
                    bufferedOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void pcmToWav(String inFilename, String outFilename) {
        pcmToWav(inFilename, outFilename, false);
    }

    /**
     * 加入wav文件头
     */
    private void writeWaveFileHeader(BufferedOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; // WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd'; // data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
