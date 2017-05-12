打包后的文件详见
---

`/ownCloud/小游戏/SDK及工具/有米广告/`

使用说明(重新打包)
---

修改`MainActivity`后点击android Studio右侧的gradle，在other里面选buildJar然后在工程
的输出文件夹里找Jars/youmi_sdk_android.jar文件，并参考[官方教程](https://app.youmi.net/sdk/android/17/doc/701/6/cn/%E6%9C%89%E7%B1%B3AndroidSDK%E4%B9%8BUnity3d%E4%BD%BF%E7%94%A8%E6%95%99%E7%A8%8B%E6%96%87%E6%A1%A3.html)使用。


**注意** 
   + 官方教程里的替换Activity的方法已经不适用。
   + 我们使用 `com.wawagame.app.youmiad.MainActivity`作为unity android的入口Activity。
   + 手动merge manifest.xml文件时需要小心。