# TinyServer

TinyServer使用方法：

1. 修改src/main/resource/ServerConfig.xml文件，将staticRoot/locationpath修改为硬盘上任意一个文件夹(作为FileManager的根目录)的绝对路径
2. 运行ServerRunner.java中的main方法，等待服务器开启
3. 在浏览器中输入localhost:8083/static/  , 应该可以看到FileManager的界面。在这个界面中可以上传文件，也可以点击任意文件或文件夹浏览。

TinyServer manual:
1. Modify src/main/resource/ServerConfig.xml, Replace the content of staticRoot/locationpath to the root directory of FileManager.
2. Run the main method of ServerRunner.java, wait for the Server running.
3. Users can input localhost:8083/static/ in address bar. The FileManager index will be shown. Users are able to upload, downloads files.
