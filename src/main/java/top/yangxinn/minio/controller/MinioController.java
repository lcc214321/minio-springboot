package top.yangxinn.minio.controller;

import top.yangxinn.minio.common.result.Result;
import top.yangxinn.minio.common.result.ResultUtil;
import top.yangxinn.minio.config.MinioConfig;
import top.yangxinn.minio.service.MinioService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/minio")
@RestController
@Api(tags = "文件上传接口")
public class MinioController {

    @Autowired
    private MinioService minioService;

    @Autowired
    private MinioConfig minioConfig;

    @Value("${yang.tmp.file}")
    private String tmpPath;

    @ApiOperation(value = "使用minio文件上传")
    @PostMapping("/uploadFile")
    @ApiImplicitParams({
            @ApiImplicitParam(dataType = "MultipartFile", name = "file", value = "上传的文件", required = true),
            @ApiImplicitParam(dataType = "String", name = "bucketName", value = "对象存储桶名称", required = false)
    })
    public Result uploadFile(MultipartFile file, String bucketName) {
        try {
            bucketName = StringUtils.isNotBlank(bucketName) ? bucketName : minioConfig.getBucketName();
            if (!minioService.bucketExists(bucketName)) {
                minioService.makeBucket(bucketName);
            }
            String fileName = file.getOriginalFilename();
            String newName = new SimpleDateFormat("yyyy/MM/dd/").format(new Date())
                    + UUID.randomUUID().toString().replaceAll("-", "");
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
            String objectName = newName + "." + suffix;
            InputStream inputStream = file.getInputStream();
            minioService.putObject(bucketName, objectName, inputStream);

            /*
             * 压缩
             */
            if (isPicture(suffix)) {
                String objectNameThumb = newName + "-thumb." + suffix;
                File newFile = new File(tmpPath + "tmp." + suffix);
                InputStream inputStreamT = file.getInputStream();
                Thumbnails.of(inputStreamT).forceSize(100,100).toFile(newFile);
                FileInputStream inputStreamThumb = new FileInputStream(newFile);
                minioService.putObject(bucketName, objectNameThumb, inputStreamThumb);
                newFile.delete();
                inputStreamT.close();
                Map<String, String> data = new HashMap<>();
                data.put("image", minioService.getObjectUrl(bucketName, objectName));
                data.put("image_thumb", minioService.getObjectUrl(bucketName, objectNameThumb));
                return ResultUtil.success(data);
            } else {
                inputStream.close();
                return ResultUtil.success(minioService.getObjectUrl(bucketName, objectName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.sendErrorMessage("上传失败");
        }
    }

    /**
     * 判断文件是否为图片
     */
    private boolean isPicture(String imgName) {
        boolean flag = false;
        if (StringUtils.isBlank(imgName)) {
            return false;
        }
        String[] arr = {"bmp", "dib", "gif", "jfif", "jpe", "jpeg", "jpg", "png", "tif", "tiff", "ico"};
        for (String item : arr) {
            if (item.equals(imgName)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

}