### 轻量级分布式应用框架 

#### 环境
* openjdk 17+
* gradle 7.2+
* ant 1.10+ (模块开发）
* Eclipse ｜ vscode 


#### 支持MVC
* 支持Web的MVC开发。
* View支持Velocity，FreeMaker；同时也支持完全分离的前后端开发。

#### 模块化
* 模块之间可以 *复用*和*重载*。
* 最大化的复用代码，更加容易的构建跨项目之间的模块共用。



#### 快速开发
* 直观简单的程序入口/出口，更容易开发和维护。
* 避免冗重的设计模式，更加快速的构建模块，快速开发和部署。



#### 轻量级分布式
* 并行对等分布式计算节点关系。
* 更容易从一个节点扩展到N个节点，构建可伸缩的分布式并行计算环境。
* 支持分布式任务， 支持分布式锁， 分布式文件系统。


#### 安装使用
* [下载giiwa](https://github.com/giiwa/giiwa/releases)

#### 获取代码
所有最新源码已经托管在Github:
> https://github.com/giiwa/giiwa.git

使用 *git clone* 源码仓库 (或者在github的官网上直接克隆)， 你就可以获得Giiwa的全部最新代码。


#### 编译和打包
使用 Gradle 编译, 她会自动编译打包所有依赖包到 giiwa.tgz, giiwa.zip 和升级模块包。
初始化eclipse环境
> gradle eclipse
编译打包
> gradle clean release -x test



#### License
giiwa 支持 [Apache V2](LICENSE-2.0.html) 许可协议。
