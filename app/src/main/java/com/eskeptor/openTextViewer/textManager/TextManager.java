package com.eskeptor.openTextViewer.textManager;

import android.util.Log;
import com.eskeptor.openTextViewer.Constant;
import com.eskeptor.openTextViewer.datatype.MemoInfo;
import util.TimeCheck;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;

/**
 * Created by eskeptor on 17. 1. 25.
 * Copyright (C) 2017 Eskeptor(Jeon Ye Chan)
 */


public class TextManager
{
    private boolean fileopen;               // 파일이 열렸는지 체크
    private boolean saved;                  // 파일이 저장되었는지 체크
    private String fileopen_name;           // 현재 열린 파일의 이름
    private String MD5;                     // 파일의 MD5값
    private String format;                  // 파일의 포멧
    private long fileSize;                  // 파일의 크기

    // 향상된 파일열기용
    private int prevPointer;                // 이전 포인터(배열 위치)
    private int curPointer;                 // 현재 포인터(배열 위치)
    private int nextPointer;                // 다음 포인터(배열 위치)
    private ArrayList<Long> pointerList;    // randomAccessFile의 포인터를 저장할 배열리스트
    private int lines;                      // 향상된 파일열기에서 출력할 라인
    private float progress;                 // 진행률

    public TextManager()
    {
        initManager();
    }

    public void initManager()
    {
        fileopen = false;
        fileopen_name = "";
        saved = false;
        MD5 = "";
        format = "";
        progress = 0;
        if(pointerList == null)
            pointerList = new ArrayList<>();
        else
            pointerList.clear();
        prevPointer = 0;
        curPointer = 0;
        nextPointer = 0;
        fileSize = 0;
        lines = 0;
    }

    public String getFileopen_name()
    {
        return fileopen_name;
    }

    public boolean isFileopen()
    {
        return fileopen;
    }

    public String getMD5()
    {
        return MD5;
    }

    public boolean saveText(final String _strData, final String _filename, final boolean _enhance)
    {
        if(_strData == null || _strData.isEmpty())
        {
            return false;
        }
        RandomAccessFile randomAccessFile = null;
        FileOutputStream fos = null;
        FileChannel channel = null;
        ByteBuffer buffer = null;

        if(isFileopen())
        {
            try
            {
                buffer = ByteBuffer.allocateDirect(_strData.getBytes().length);
                if(_enhance)
                {
                    randomAccessFile = new RandomAccessFile(new File(fileopen_name), "rw");
                    channel = randomAccessFile.getChannel();
                    randomAccessFile.seek(pointerList.get(curPointer));
                }
                else
                {
                    fos = new FileOutputStream(new File(fileopen_name));
                    channel = fos.getChannel();
                }

                buffer.put(_strData.getBytes());
                buffer.flip();
                channel.write(buffer);
            }
            catch (Exception e) {e.printStackTrace();}
            finally {
                if (buffer != null) {
                    try {
                        buffer.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else
        {
            try
            {
                buffer = ByteBuffer.allocateDirect(_strData.getBytes().length);
                if(_enhance)
                {
                    randomAccessFile = new RandomAccessFile(new File(_filename), "rw");
                    channel = randomAccessFile.getChannel();
                }
                else
                {
                    fos = new FileOutputStream(new File(_filename));
                    channel = fos.getChannel();
                }
                buffer.put(_strData.getBytes());
                buffer.flip();
                channel.write(buffer);
            }
            catch (Exception e) {e.printStackTrace();}
            finally {
                if (buffer != null) {
                    try {
                        buffer.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        saved = true;
        return true;
    }

    public String openText(final String _filename, final int _sens, final boolean _enhance, final int _format)
    {
//        TimeCheck checker = new TimeCheck("openText");
        if(_filename != null)
        {
            RandomAccessFile randomAccessFile = null;
            FileInputStream fis = null;
            FileChannel channel = null;
            ByteBuffer byteBuffer = null;
            StringBuilder stringBuilder = new StringBuilder();

            int _lines = lines;

            try
            {
                if(_enhance)
                {
//                    checker.CheckStart();
                    randomAccessFile = new RandomAccessFile(new File(_filename), "r");
                    channel = randomAccessFile.getChannel();
                    byteBuffer = ByteBuffer.allocateDirect(12);
                    String tmp;
                    fileSize = randomAccessFile.length();
                    if(randomAccessFile.length() != 0)
                    {
                        channel.read(byteBuffer);
                        byteBuffer.flip();
                        byteBuffer.clear();


                        if(nextPointer == 0)
                        {
                            prevPointer = 0;
                            curPointer = 0;
                            pointerList.add(0, 0L);
                            nextPointer++;
                        }
                        else
                        {
                            if(_sens == Constant.MEMO_BLOCK_NEXT)
                            {
                                prevPointer = curPointer;
                                curPointer = nextPointer;
                                nextPointer++;
                            }
                            else
                            {
                                nextPointer = curPointer;
                                curPointer = prevPointer;
                                if(prevPointer != 0)
                                    prevPointer--;
                            }
                        }
                        randomAccessFile.seek(pointerList.get(curPointer));

                        while((tmp = randomAccessFile.readLine()) != null)
                        {
                            Charset utf = Charset.forName("ISO-8859-1");
                            byteBuffer = utf.encode(tmp);
                            if(_format == Constant.ENCODE_TYPE_EUCKR)
                            {
                                stringBuilder.append(new String(byteBuffer.array(), Constant.ENCODE_TYPE_EUCKR_STR));
                            }
                            else
                            {
                                stringBuilder.append(new String(byteBuffer.array()));
                            }
                            stringBuilder.append("\n");
                            if((--_lines) == 0)
                            {
                                break;
                            }
                        }

                        try{
                            pointerList.get(nextPointer);
                        }catch (IndexOutOfBoundsException ioobe) {
                            pointerList.add(nextPointer, randomAccessFile.getFilePointer());
                        }

                        fileopen = true;
                        fileopen_name = _filename;
                        MD5 = createMD5(stringBuilder.toString());
//                        checker.CheckEnd();
//                        Log.e("Debug", "openText");
//                        Log.e("Debug", "prevPointer:" + pointerList.get(prevPointer) + "," + Integer.toString(prevPointer));
//                        Log.e("Debug", "curPointer:" + pointerList.get(curPointer) + "," + Integer.toString(curPointer));
//                        Log.e("Debug", "nextPointer:" + pointerList.get(nextPointer) + "," + Integer.toString(nextPointer));

                        if(pointerList.get(nextPointer) == fileSize)
                            progress = 100.0F;
                        else
                            progress = (float)pointerList.get(curPointer) / (float)fileSize * 100;

//                        Log.e("Debug", checker.CheckResult());
                        return new String(stringBuilder);
                    }
                    else
                    {
                        fileopen = false;
                        fileopen_name = "";
                    }
                }
                else
                {
                    fis = new FileInputStream(new File(_filename));
                    channel = fis.getChannel();
                    byteBuffer = ByteBuffer.allocateDirect((int)channel.size());
                    if(fis.available() != 0)
                    {
                        channel.read(byteBuffer);
                        byteBuffer.flip();
                        if(formatDetector(byteBuffer) != null)
                        {
                            format = Constant.ENCODE_TYPE_UTF8_STR;
                        }
                        else
                        {
                            format = Constant.ENCODE_TYPE_EUCKR_STR;
                        }
//                        Log.e("Debug", "format:" + format);
                        fileopen = true;
                        fileopen_name = _filename;
                        MD5 = createMD5(byteBuffer.array(), _enhance);
                        return new String(byteBuffer.array(), format);
                    }
                    else
                    {
                        fileopen = false;
                        fileopen_name = "";
                    }
                }
            }
            catch (Exception e){e.printStackTrace();}
            finally {
                if(byteBuffer != null) {
                    try {
                        byteBuffer.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(channel != null) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return "";
    }

    public String createMD5(final byte[] _message, final boolean _enhance)
    {
        MessageDigest messageDigest;
        StringBuilder sbuilder = new StringBuilder();
        CharBuffer charBuffer = null;
        try
        {
            messageDigest = MessageDigest.getInstance("MD5");
            if(!_enhance)
            {
                charBuffer = Charset.forName(format).newDecoder().decode(ByteBuffer.wrap(_message));
                messageDigest.update(charBuffer.toString().getBytes());
            }
            else
            {
                messageDigest.update(_message);
            }
            byte[] hash = messageDigest.digest();
            for (int i = 0; i < hash.length; i++)
            {
                sbuilder.append(String.format("%02x", hash[i] & 0xff));
            }
        }
        catch (Exception e){e.printStackTrace();}
        finally {
            if(!_enhance)
            {
                if (charBuffer != null) {
                    try {
                        charBuffer.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return sbuilder.toString();
    }

    public String createMD5(final String _message)
    {
        MessageDigest messageDigest;
        StringBuilder sbuilder = new StringBuilder();
        try
        {
            messageDigest = MessageDigest.getInstance("MD5");
            // 타입에 상관없이 UTF8인 이유는 이미 UTF8로 변환되어있는 것을 체크하기 때문
            messageDigest.update(_message.getBytes(Charset.forName(Constant.ENCODE_TYPE_UTF8_STR)));
            byte[] hash = messageDigest.digest();
            for (int i = 0; i < hash.length; i++)
            {
                sbuilder.append(String.format("%02x", hash[i] & 0xff));
            }
        }
        catch (Exception e){e.printStackTrace();}
        return sbuilder.toString();
    }

    private CharBuffer formatDetector(final ByteBuffer _buffer)
    {
        CharBuffer charBuffer = null;
        try
        {
            CharsetDecoder decoder = Charset.forName(Constant.ENCODE_TYPE_UTF8_STR).newDecoder();
            charBuffer = decoder.decode(_buffer);
        }
        catch (CharacterCodingException cce)
        {
            return null;
        }
        return charBuffer;
    }

    public boolean isNext()
    {
        return pointerList.get(nextPointer) != fileSize;
    }
    public boolean isPrev()
    {
        return pointerList.get(prevPointer) != (pointerList.get(curPointer));
    }
    public void setLines(final int _lines)
    {
        lines = _lines;
    }
    public float getProgress()
    {
        return progress;
    }
}