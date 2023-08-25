# Cloud Printing

外卖云打印机聚合接口。

如果你只是需要 API 的简单封装，可以考虑使用 [Cloud Printing Api](https://github.com/xesam/cloud-printing-api)。

## 当前支持的打印产商

1. 芯烨；
2. 飞鹅；
3. 商鹏。

*注意：这些厂商各自都有常规打印机与云打印机系列，只有**云打印机**型号才被支持*

## 使用说明

### 1、创建 CloudApi 对象

```java
CloudApi cloud1 = new FeieCloud(new CloudAuth("飞鹅账号", "飞鹅账号sceret")); // 飞鹅
CloudApi cloud2 = new XpyunCloud(new CloudAuth("芯烨云账号", "芯烨云账号sceret"));// 芯烨云
CloudApi cloud3 = new SpyunCloud(new CloudAuth("商鹏云账号", "商鹏云账号sceret"));// 商鹏云
```

有的厂商支持打印回调，如果你需要配置打印回调地址，则需要在创建 CloudApi 对象的时候指定：

```java
CloudApi cloud1 = new FeieCloud(new CloudAuth("飞鹅账号", "飞鹅账号sceret"), "对调地址配置"); // 飞鹅
CloudApi cloud2 = new XpyunCloud(new CloudAuth("芯烨云账号", "芯烨云账号sceret"), "对调地址配置");// 芯烨云
```

### 2、创建设备对象

```java
Device device = new Device("551506419")
    .setKey("t7rrdvax");
```
其他支持属性参见 [Device.java](src/main/java/com/github/xesam/printing/cloud/Device.java)

*不同厂商的属性差异较大，暂时只取属性交集。*

### 3、创建订单对象

```java
//创建一个打印单
Order order = new Order()
    .setContent("初始化成功");
```
其他支持属性参见 [Order.java](src/main/java/com/github/xesam/printing/cloud/Order.java)

*不同厂商的属性差异较大，暂时只取属性交集。*

### 4、根据具体需求调用

```java
//查询指定设备
CloudResponse<Device> cloudResponse = cloud.queryDevice(device);
if(cloudResponse.isSuccess()){// 是否成功查询
    cloudResponse.getSuccessEntity().isOnline(); // 打印机 是否在线
    cloudResponse.getSuccessEntity().getStatus(); // 打印机 具体状态
}else{
    Logger.error("失败原因", cloudResponse.getFailMessage());
}

//打印小票单
CloudResponse<Order> cloudResponse = cloud.printMsgOrder(device, order);
if(cloudResponse.isSuccess()){// 是否成功查询
    Order order = cloudResponse.getEntity();
    order.getId();//得到小票单的服务器id
}else{
    Logger.error("失败原因", cloudResponse.getFailMessage());
}
//根据订单 Id 查询订单的打印情况
CloudResponse<Order> orderRes = cloud.queryOrder(order);
if(orderRes.isSuccess()){// 是否成功查询
    cloudResponse.getSuccessEntity().isPrinted(); // 此订单是否打印成功
}else{
    Logger.error("失败原因", orderRes.getFailMessage());
}
```

**小票机只能打印小票订单，标签机只能打印标签订单，如果打印失败，优先检查型号是否正确。**

## 接口说明

参见 CloudApi 接口定义：[CloudApi.java](src/main/java/com/github/xesam/printing/cloud/CloudApi.java)

## 厂商接口差异

服务端打印回调 差异：

| 平台差异   | 飞鹅 | 商鹏云 | 芯烨云 |
| -------- | ---- | ------ | ------ |
| 服务器回调 | 支持 | 不支持 | 支持 |

常规接口 差异：

| 接口   | 飞鹅 | 商鹏云 | 芯烨云 |
| -------- | ---- | ------ | ------ |
| 打印小票 | 支持 | 支持 | 支持 |
| 打印标签 | 支持 | 不支持 | 支持 |
| 打印超时 | 支持接口配置，最长24小时 | 固定48小时 | 支持接口配置，最长24小时 |
| 添加设备 | 支持   | 支持  |  支持     |
| 删除设备 | 支持   | 支持  |  支持     |
| 修改设备 | 支持   | 支持  |  支持     |
| 查询订单 | 支持   | 支持  |  支持     |
| 清空订单 | 支持   | 支持  |  支持     |
| 修改声音 | 不支持 | 支持  |  支持     |
| 配置切刀 | 不支持 | 支持  |  不支持   |

## 依赖与自定义

依赖项：

    httpclient5 发送 http 请求
    jackson 解析云商接口响应

因此，需要添加对应的依赖：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.14.0</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.5</version>
</dependency>
```

### httpclient5 设置

如果你只想修改 httpclient5 的选项，可以自定义一个 CloseableHttpClient：

```java
CloseableHttpClient cusomHttpClient = HttpClients.custom().build();
SimpleRequestClient newRequestClient = new SimpleRequestClient(cusomHttpClient)
CloudApi cloud = new FeieCloud(new CloudAuth("xxx", "yyy")).setRequestClient(newRequestClient);
```

如果你想完全替换掉 httpclient5，那就根据你自己所选择的 http 支持库，实现 [RequestClient 接口](src/main/java/com/github/xesam/printing/cloud/RequestClient.java) 即可：

```java
RequestClient newRequestClient = new CustomRequestClient()
CloudApi cloud = new FeieCloud(new CloudAuth("xxx", "yyy")).setRequestClient(newRequestClient);
```

### 替换 jackson

由于 JSON 解析比较单一，所以没有提供配置的空间，如果你想配置，实现 [ResponseParser 接口](src/main/java/com/github/xesam/printing/cloud/ResponseParser.java) 即可：

```java
ResponseParser newResponseParser = new CustomResponseParser()
CloudApi cloud = new FeieCloud(new CloudAuth("xxx", "yyy")).setResponseParser(newResponseParser);
```

## 问题反馈

[xesam@outlook.com](mailto://xesam@outlook.com)

## ChangeLog

### 0.0.1
1. 通用请求支持。
