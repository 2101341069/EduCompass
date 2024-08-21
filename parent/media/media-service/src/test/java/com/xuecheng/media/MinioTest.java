package com.xuecheng.media;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.io.IOUtil;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MinioTest {

    public static void main(String[] args)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, ServerException, InsufficientDataException, ErrorResponseException, InvalidResponseException, XmlParserException, InternalException {
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint("http://192.168.101.65:9000")
                            .credentials("minioadmin", "minioadmin")
                            .build();
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
            String mimeType="";
            if (extensionMatch!=null){
                 mimeType = extensionMatch.getMimeType();
            }
            // Make 'asiatrip' bucket if not exist.
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("testbucket").build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("testbucket").build());
            } else {
                System.out.println("Bucket 'testbuild' already exists.");
            }

            // Upload '/home/user/Photos/asiaphotos.zip' as object name 'asiaphotos-2015.zip' to bucket
            // 'asiatrip'.
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("testbucket")
                            .object("test/01/test.zip")
                            .filename("D:\\leetcode-editor-pro-2023.1.0.zip")
                            .contentType(mimeType)
                            .build());
            System.out.println(
                    "'/home/user/Photos/asiaphotos.zip' is successfully uploaded as "
                            + "object 'asiaphotos-2015.zip' to bucket 'asiatrip'.");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }
    @Test
    public void test() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // Create a minioClient with the MinIO server playground, its access key and secret key.
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint("http://192.168.101.65:9000")
                        .credentials("minioadmin", "minioadmin")
                        .build();
        RemoveObjectArgs testbucket =
                RemoveObjectArgs.builder().bucket("testbucket").object("test.zip").build();
        minioClient.removeObject(testbucket);
    }
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void getFile() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        GetObjectArgs build = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("/test/01/test.zip").build();
        FilterInputStream object = minioClient.getObject(build);
        FileOutputStream outputStream=new FileOutputStream("D:\\test.zip");
        IOUtils.copy(object,outputStream);
        String s = DigestUtils.md5Hex(object);
        String s1 = DigestUtils.md5Hex(new FileInputStream("D:\\test.zip"));
        if (s.equals(s1)){
            System.out.println("下载成功");
        }
    }

    @Test
    public void getFile1() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        GetObjectArgs build = GetObjectArgs.builder()
//                .bucket("testbucket")
//                .object("/test/01/test.zip").build();
//        FilterInputStream object = minioClient.getObject(build);
        FileInputStream outputStream=new FileInputStream("D:\\leetcode-editor-pro-2023.1.0.zip");
        String s = DigestUtils.md5Hex(outputStream);
        String s1 = DigestUtils.md5Hex(new FileInputStream("D:\\test.zip"));
        if (s.equals(s1)){
            System.out.println("下载成功");
        }
    }

    @Test
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        File chunkFolder=new File("D:\\chunk");
        File[] files = chunkFolder.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        for (int i = 0; i < files.length; i++) {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("testbucket")
                            .object("chunk/"+i)
                            .filename(files[i].getAbsolutePath())
                            .build());
            System.out.println("上车分块"+i);
        }

    }
    @Test
    public void mergeChunk() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
//        List<ComposeSource> list=new ArrayList<>();
//        for (int i = 0; i < 89; i++) {
//            ComposeSource testbucket = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
//            list.add(testbucket);
//        }
        List<ComposeSource> list = Stream.iterate(0, i -> i++).limit(17)
                .map(item -> ComposeSource.builder().bucket("testbucket").object("chunk/" + item).build())
                .collect(Collectors.toList());
        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merger01.csv")
                .sources(list)
                .build();
        minioClient.composeObject(testbucket);
    }

}
