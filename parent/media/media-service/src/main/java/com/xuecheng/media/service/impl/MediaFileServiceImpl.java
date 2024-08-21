package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.execption.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MediaFileService mediaFileService;

    //存储普通文件
    @Value("${minio.bucket.files}")
    private String bucket_files;

    //存储视频文件
    @Value("${minio.bucket.videofiles}")
    private String bucket_videos;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams,
                                                  QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName) {
        String fileName = uploadFileParamsDto.getFilename();
        //后缀名
        String extension = fileName.substring(fileName.lastIndexOf("."));
        //得到mimetype类型

        String defaultFolderPath = getDefaultFolderPath();
        String fileMd5 = getFileMd5(new File(localFilePath));
        if (StringUtil.isEmpty(objectName)) {
            objectName = defaultFolderPath + fileMd5 + extension;
        }
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_files, objectName);
            String mimeType = getMimeType(extension);
            if (mediaFiles == null) {
                XueChengPlusException.cast("上传文件失败");
            }
            boolean result = addMediFileToMinIo(bucket_files, mimeType, objectName, localFilePath);
            if (!result) {
                XueChengPlusException.cast("上传文件失败");
            }
        }
        UploadFileResultDto resultDto = new UploadFileResultDto();

        BeanUtils.copyProperties(mediaFiles, resultDto);
        return resultDto;
    }

    private boolean checkExistMediaFile(String objectName) {
        try {
           minioClient.statObject(StatObjectArgs.builder().bucket(bucket_files).object(objectName).build());
        } catch (Exception e) {
            return false;
        }
        return true;
    }



    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

        GetObjectArgs build = GetObjectArgs.builder()
                .bucket(bucket_videos)
                .object(chunkFileFolderPath).build();

        try {
            FilterInputStream filterInputStream = minioClient.getObject(build);
            if (filterInputStream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        return RestResponse.success(false);
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {

        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles != null) {
            GetObjectArgs build = GetObjectArgs.builder().bucket(mediaFiles.getBucket()).object(mediaFiles.getFilePath()).build();

            try {
                FilterInputStream filterInputStream = minioClient.getObject(build);
                if (filterInputStream != null) {
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        String mimeType = getMimeType(null);
        String objectName = getChunkFileFolderPath(fileMd5) + chunk;
        boolean b = addMediFileToMinIo(bucket_videos, mimeType, objectName, localChunkFilePath);
        if (!b) {
            return RestResponse.validfail(false, "上传分开失败");
        }
        return RestResponse.success(true, "上传分开成功");
    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);


        List<ComposeSource> list = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(item -> ComposeSource.builder().bucket(bucket_videos).object(chunkFileFolderPath + item).build())
                .collect(Collectors.toList());

        String filename = uploadFileParamsDto.getFilename();

        String extension = filename.substring(filename.lastIndexOf("."));
        String objectName = getFilePathByMd5(fileMd5, extension);

        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket(bucket_videos)
                .object(objectName)
                .sources(list)
                .build();

        try {
            minioClient.composeObject(testbucket);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息：{}", bucket_videos, objectName, e.getMessage());
            return RestResponse.validfail(false, "合并文件异常");
        }
        File file = downloadFileFromMinIO(bucket_videos, objectName);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String mergerFileMd5 = DigestUtils.md5Hex(fileInputStream);
            if (!mergerFileMd5.equals(fileMd5)) {
                log.error("校验合并文件md值不一致");
                return RestResponse.validfail(false, "文件校验失败异常");
            }
            uploadFileParamsDto.setFileSize(file.length());
        } catch (Exception e) {
            return RestResponse.validfail(false, "合并文件异常");
        }
        MediaFiles mediaFiles =
                mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_videos, objectName);
        ;
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "文件入库失败");
        }

        clearChunkFiles(chunkFileFolderPath, chunkTotal);

        return RestResponse.success(true);
    }

    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_videos).objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }

    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private String getFilePathByMd5(String fileMd5, String extension) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + extension;
    }

    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //拷贝基本信息
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setUrl("/" + bucket + "/" + objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.debug("向数据库保存文件信息失败，bucket：{}，objectName：{}", bucket_files, objectName);
                return null;
            }
            addWaitingTask(mediaFiles);
        }
        return mediaFiles;

    }

    /**
     * 添加待处理任务
     *
     * @param mediaFiles 媒资文件信息
     */
    private void addWaitingTask(MediaFiles mediaFiles) {
        //获取文件的mimetype
        String filename = mediaFiles.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        if (mimeType.equals("video/x-msvideo")) {//video/x-msvideo
            // if(mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            mediaProcess.setFileId(mediaFiles.getFileId());
            mediaProcess.setFilename(filename);
            mediaProcess.setBucket(mediaFiles.getBucket());
            mediaProcess.setFilePath(mediaFiles.getFilePath());
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);//失败次数
            mediaProcessMapper.insert(mediaProcess);
        }
    }

    private String getDefaultFolderPath() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = simpleDateFormat.format(new Date());
        return dateString + "/";
    }

    private String getMimeType(String extension) {
        if (extension == null) {
            extension = "";
        }
        ContentInfo mimeTypeMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (mimeTypeMatch != null) {
            mimeType = mimeTypeMatch.getMimeType();
        }
        return mimeType;
    }

    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }

    public boolean addMediFileToMinIo(String bucketName, String minType, String objectName, String localFilePath) {
        try {
            UploadObjectArgs build = UploadObjectArgs
                    .builder()
                    .bucket(bucketName)
                    .filename(localFilePath)
                    .object(objectName)
                    .contentType(minType).build();
            minioClient.uploadObject(build);
            log.debug("上传文件到minio成功，bucket:{}，文件名为：{}", bucketName, localFilePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件到minio失败，bucket:{}，文件名为：{},错误信息：{}", bucketName, localFilePath, e.getMessage());
            return false;
        }


    }
}
