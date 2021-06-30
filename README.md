# autocall

#### 介绍
AutoCall是一款安卓手机APP，用于运维场景告警根据接口自动拨打电话，支持是否接通回调。同时支持根据短信内容拨打电话，适用于对外一个号码，定时设置呼叫转移场景。

![avatar](https://oscimg.oschina.net/oscnet/up-75424d3fa1f38b7af2a914cfa42a6def1ba.png)
![avatar](https://oscimg.oschina.net/oscnet/up-f566ba2159f1212470aeaf4fa25aff8aa3d.png)
![avatar](https://oscimg.oschina.net/oscnet/up-1d8358320fd5b328f75adb8e7a1c47d8e69.png)

#### 软件架构
1. 定时循环告警电话接口拨打电话，拨打完成回调接口设置是否接通
2. 监听短信提取号码拨打


#### 安装教程

1. 使用idea打开编译生成apk安装,或见APP百度网盘下载地址：https://pan.baidu.com/s/1SJtY9U3K54ISbwo9HiMuSg
2. 服务端接口参考使用说明实现自行结合业务场景完善,已有案例是接通后挂断查看微信或短信告警并显示谁接通,如未接通则显示告警无响应
3. 遇到问题：微信搜索ahauzyy


#### 使用说明

**1. 告警电话列表请求接口**
```GET请求：
curl 'http://192.168.1.8:8080/alert/phone' 
有告警返回值：
{"phone": ["18712345678", "18812345678","18712345678", "18812345678"], "code": 0, "mid": "60d536885fa2a8643fa4d3a0"}
无告警返回值：
{"code": 1, "msg":"无告警"}
备注：该接口一次只能读取一条告警,不能有相邻号码重复,未接通拨打多遍按顺序复制
```
**2. 电话接通调用接口**
```POST请求：
curl -X POST 'http://192.168.1.8:8080/alert/phone?mid=60d536885fa2a8643fa4d3a0'
备注：请求该接口需要自行标记告警电话已接受处理，完成全部拨打此时无人响应

curl -X POST 'http://192.168.1.8:8080/alert/phone?mid=60d536885fa2a8643fa4d3a0&resp=18712345678' 
备注：请求该接口需要自行标记告警电话已接受处理，完成全部拨打此时响应号码为resp
返回值自行定义：
{"code": 0}
```
**3. 服务端代码示例**


 

#基于tornado+mongodb代码示例，autocall为电话队列库，asa为告警消息库，duty为值班库，自行结合业务修改
 
    class AlertPhoneHandler(BaseMisHandler):
     def get(self):
        res={}
        phones=[]
        #读取一条未处理告警，该告警包含电话号码
        info=mgdb.autocall.find_one({'iscall': 0})
        if not info:
            self.write(result_dic(1,"无告警"))
            return
        res['mid']=str(info['_id'])
        #结合cmdb去重，结合业务场景自行完善
        if info['cmdb_ops']:
            phones.append(info['cmdb_ops'])
        if info['cmdb_owner']:
            phones.append(info['cmdb_owner'])
        phones.extend(info['owners'])
        res['phone']=phones
        res['code']=0
        #此处可以增加手机心跳是否存活相关发短信逻辑
        self.write(res)
        
    def post(self):
        mid = self.get_argument('mid', None)
        resp = self.get_argument('resp', None)
        #标记电话已处理
        record={'iscall':1}
        mgdb.autocall.update({'_id': ObjectId(mid)},{'$set': record},upsert=False)
        if resp:
            info=mgdb.autocall.find_one({"_id":ObjectId(mid)})
            #响应人读取值班库转换成人名
            tel=mgdb.duty.find_one({"telNo":resp})
            if tel:
                record_asa={'response':tel.get('name',resp)}
            else:
                record_asa={'response':resp}
            logging.info(info)
            #标记告警对否响应
            mgdb.asa.update({'_id': ObjectId(info['asa_id'])},{'$set': record_asa},upsert=False)
        self.write(result_dic(0))


**4. 发送短信拨打号码**
```
短信格式：|呼叫号码|autocall|自定义内容
举例：|18712345678|autocall|今天你值班
适用场景，对外一个号码，改号码收到短信自动设置呼叫转移为值班人员
电信呼叫转移：
|*7218712345678|autocall|今天你值班，已设置呼叫转移为你
【电信是*72,其它运营商的sim可请自行查询】
```
**5. APP设置**
```
权限：允许短信、电话、联系人、网络相关权限
填写接口地址和其它设置，【接口地址不支持域名】
```

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技


