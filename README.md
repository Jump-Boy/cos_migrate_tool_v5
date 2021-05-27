# 迁移工具

>项目初始支持的周期性执行情况：支持每天在特定窗口时间内周期执行，并每天重复执行。（daemonMode on的情况下）
现结合自身需求，将源码修改，改为：在特定时间窗口内周期间隔执行（daemonMode on的情况下），仅执行一次时间窗口，并取消时间窗口一天内的限制，可跨天执行，如2021-05-26 10:00 - 2021-05-28 22:00 之间每隔5分钟执行一次同步！

**项目基于[tencentyun/cos_migrate_tool_v5](https://github.com/tencentyun/cos_migrate_tool_v5)修改，原始内容可自行参考**

## 功能说明

迁移工具集成了有关COS数据迁移的功能, 目前支持以下四大类迁移
- 本地数据迁移到COS, 功能同之前的本地同步工具
- 友商数据迁移到COS, 目前支持aws s3, 阿里云oss, 七牛存储, 又拍云存储
- 根据url下载列表进行下载迁移
- COS的bucket数据相互复制, 支持跨账号跨地域的数据复制

## 运行依赖
- JDK1.8或以上, 有关JDK的安装请参考[JAVA安装与配置](https://cloud.tencent.com/document/product/436/10865)
- linux或windows环境, 推荐linux

# 使用范例
1. 配置全部通过配置文件读入
sh start_migrate.sh
2. 指定部分配置项以命令行为主.
sh start_migrate.sh -DmigrateLocal.localPath=/test_data/aaa/ -Dcommon.cosPath=/aaa
sh start_migrate.sh -DmigrateAws.prefix=/test_data/bbb/ -Dcommon.cosPath=/bbb

## 迁移机制

迁移工具是有状态的，已经迁移成功的会记录在db目录下，以KV的形式存储在leveldb文件中. 
每次迁移前对要迁移的路径, 先查找下DB中是否存在, 如果存在，且属性和db中存在的一致, 则跳过迁移, 否则进行迁移。这里的属性根据迁移类型的不同而不同，对于本地迁移，会判断mtime。对于友商与bucket复制，会判断源文件的etag和长度是否与db一致。
因此，我们参照的db中是否有过迁移成功的记录，而不是查找COS，如果绕过了迁移工具，通过别的方式(比如coscmd或者控制台)删除修改了文件，那么运行迁移工具由于不会察觉到这种变化，是不会重新迁移的。

## 其他
请参照COS迁移工具[官网文档](https://cloud.tencent.com/document/product/436/15392)


## 补充 oss - cos 逻辑

batchTaskPath: 可以将cos配置写入单独文件中。而不在项目配置config.ini中配置

在一次大task（App.main）结束时，进行周期睡眠，如睡5分钟（daemonModeInterVal），在大task中的每个小task线程，运行时会进行时间窗口的检查，如果不满足，会睡眠，直到满足。

oss遍历失败（1000文件1遍历），如果遍历到第2次失败，那么会紧接着重新第2次遍历，而不是从从头开始（com.qcloud.cos_migrate_tool.task.MigrateAliTaskExecutor.buildTask 中ObjectListing为循环外的变量）

HistoryRecordDb 里存储已经上传的记录

config.ini中的"resume"解释：如果resume为true，则oss list遍历的时候，会紧接从上一次成功迁移的文件开始，这里是根据oss nextMarker机制，列举名称字母序排在marker之后的Bucket。
所以对于我们的场景，oss指定路径，会不断新增文件，不一定新增的文件都是在某个marker之后，比如上次记录在b1.gz，那么resume为true时，则只会遍历b12或b2，c等等。
但我们可能新增a2。所以需要关闭resume，从头遍历即可。反正上传时，也会从db中判断是否上传过。

resume=true 适合对那些路径下内容不变的资源进行上传。

文件再上传cos前，会先通过接口获取对应cos元数据   realTimeCompare