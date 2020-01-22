## 2.0版本

### 直播流程

    1. 对接processor转为对接distributor
    2. 直播地址以及录像地址由distributor进行管理
    3. 每更新拉流liveId需要通知distributor

### 连线流程

    1. 同意连线时，将主播、观众以及反推的liveId都发送给processor，同时roomManager管理这三个liveId,断开连线即通知processor关闭房间，取消连线者和反推的liveId维护
    2. 同意连线或者断开连线通知distributor更新拉流的liveId
    3. 同意连线或者断开连线通过ws通知pcClient