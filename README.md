# Linux 下安装MinIO

```linux
# 下载
wget http://dl.minio.org.cn/server/minio/release/linux-amd64/minio

# 赋权
chmod +x minio

# 设置用户名
export MINIO_ACCESS_KEY=minio

# 设置密码
export MINIO_SECRET_KEY=miniopassword

# 启动  /home/data是自己定义的文件目录，进入minio的下载目录
./minio server /home/data

# 静默启动 
nohup ./minio server --address 0.0.0.0:10900 /home/minio/data > /home/minio/log/minio.log 2>&1 &

访问 http://yangxinn.top:10900/minio/login接口看到页面

# SpringBoot 集成MinIO


## 引入jar包
```xml
  <dependency>
  	  <groupId>io.minio</groupId>
      <artifactId>minio</artifactId>
      <version>7.0.2</version>
  </dependency>
```
## 配置
```yml
minio:
  endpoint: http://yangxinn.top
  port: 10900
  accessKey: minio
  secretKey: minioyang
  secure: false
  bucketName: "7chat"
  configDir: "/home/minio/data/"

yang:
  tmp:
    file: "/home/minio/tmp/"
```

## 图片生成缩略图
```xml
  <dependency>
      <groupId>net.coobird</groupId>
      <artifactId>thumbnailator</artifactId>
      <version>0.4.8</version>
  </dependency>
```
```java
@RequestMapping("/minio")
@RestController
public class MinioController {

    @Autowired
    private MinioService minioService;

    @Autowired
    private MinioConfig minioConfig;

    @Value("${yang.tmp.file}")
    private String tmpPath;

    @PostMapping("/uploadFile")
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
```