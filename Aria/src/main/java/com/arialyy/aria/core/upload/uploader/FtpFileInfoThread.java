/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arialyy.aria.core.upload.uploader;

import com.arialyy.aria.core.common.AbsFtpInfoThread;
import com.arialyy.aria.core.common.CompleteInfo;
import com.arialyy.aria.core.common.OnFileInfoCallback;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.upload.UploadTaskEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.util.Properties;
import org.apache.commons.net.ftp.FTPFile;

/**
 * Created by Aria.Lao on 2017/9/26.
 * 单任务远程服务器文件信息
 */
class FtpFileInfoThread extends AbsFtpInfoThread<UploadEntity, UploadTaskEntity> {
  private static final String TAG = "FtpUploadFtpFileInfoThread";
  static final int CODE_COMPLETE = 0xab1;
  private boolean isComplete = false;

  FtpFileInfoThread(UploadTaskEntity taskEntity, OnFileInfoCallback callback) {
    super(taskEntity, callback);
  }

  @Override protected String setRemotePath() {
    return mTaskEntity.getUrlEntity().remotePath + "/" + mEntity.getFileName();
  }

  /**
   * 如果服务器的文件长度和本地上传文件的文件长度一致，则任务任务已完成。
   * 否则重新修改保存的停止位置，这是因为outputStream是读不到服务器是否成功写入的。
   * 而threadTask的保存的停止位置是File的InputStream的，所有就会导致两端停止位置不一致
   *
   * @param remotePath ftp服务器文件夹路径
   * @param ftpFile ftp服务器上对应的文件
   */
  @Override protected void handleFile(String remotePath, FTPFile ftpFile) {
    super.handleFile(remotePath, ftpFile);
    if (ftpFile != null) {
      //远程文件已完成
      if (ftpFile.getSize() == mEntity.getFileSize()) {
        isComplete = true;
        ALog.d(TAG, "FTP服务器上已存在该文件【" + ftpFile.getName() + "】");
      } else {
        ALog.w(TAG, "FTP服务器已存在未完成的文件【"
            + ftpFile.getName()
            + "，size: "
            + ftpFile.getSize()
            + "】"
            + "尝试从位置："
            + (ftpFile.getSize() - 1)
            + "开始上传");
        File configFile = new File(CommonUtil.getFileConfigPath(false, mEntity.getFileName()));
        Properties pro = CommonUtil.loadConfig(configFile);
        String key = mEntity.getFileName() + "_record_" + 0;
        mTaskEntity.setNewTask(false);
        long oldRecord = Long.parseLong(pro.getProperty(key, "0"));
        if (oldRecord == 0 || oldRecord != ftpFile.getSize()) {
          //修改本地保存的停止地址为服务器上对应文件的大小
          pro.setProperty(key, (ftpFile.getSize() - 1) + "");
          CommonUtil.saveConfig(configFile, pro);
        }
      }
    }
  }

  @Override protected void onPreComplete(int code) {
    super.onPreComplete(code);
    mCallback.onComplete(mEntity.getKey(), new CompleteInfo(isComplete ? CODE_COMPLETE : code));
  }
}
