package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

public class BigFileTest {
    @Test
    public void testChunk() throws IOException {
        File sourceFile=new File("D:\\浏览器下载文件夹\\电子产品销售分析.csv");
        String checkFilePath="D:\\chunk\\";

        int chunkSize=1024*1024*5;
        int chunkNum=(int) Math.ceil(sourceFile.length()*1.0/chunkSize);
        RandomAccessFile r = new RandomAccessFile(sourceFile, "r");
        byte[] bytes=new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File file = new File(checkFilePath + i);
            RandomAccessFile w=new RandomAccessFile(file,"rw");
            int len=-1;
            while ((len=r.read(bytes))!=-1){
                w.write(bytes,0,len);
                if (file.length()>=chunkSize){
                    break;
                }
            }
            w.close();
        }
        r.close();
    }
    @Test
    public void merger() throws IOException {
        File chunkFolder=new File("D:\\chunk");
        File sourceFile=new File("D:\\浏览器下载文件夹\\电子产品销售分析.csv");
        File mergerFile=new File("D:\\1.csv");
        File[] files = chunkFolder.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        RandomAccessFile rw = new RandomAccessFile(mergerFile, "rw");
        byte[]bytes=new byte[1024];
        for (File file : files) {
            RandomAccessFile r = new RandomAccessFile(file, "rw");
            int len=-1;
            while ((len=r.read(bytes))!=-1){
                rw.write(bytes,0,len);
            }
            r.close();
        }
        rw.close();
        String s = DigestUtils.md5Hex(new FileInputStream(sourceFile));
        String s1 = DigestUtils.md5Hex(new FileInputStream(mergerFile));
        if (s.equals(s1)){
            System.out.println("合并成功");

        }
    }
}
