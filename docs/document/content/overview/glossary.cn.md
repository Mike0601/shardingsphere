+++
pre = "<b>1.2 </b>"
title = "核心概念"
weight = 2
chapter = true
+++

## Database Plus

Database Plus 是 Apache ShardingSphere 的产品定位，旨在构建异构数据库上层的标准和生态。 它关注如何充分合理地利用数据库的计算和存储能力，而并非实现一个全新的数据库。ShardingSphere 站在数据库的上层视角，关注他们之间的协作多于数据库自身。 

连接、增量 和 可插拔是 Database Plus的核心概念：

### 连接

通过对数据库协议、SQL 方言以及数据库存储的灵活适配，快速的连接应用与多模式的异构数据库；

### 增量

获取数据库的访问流量，并提供流量重定向（数据分片、读写分离、影子库）、流量变形（数据加密、数据脱敏）、流量鉴权（安全、审计、权限）、流量治理（熔断、限流）以及流量分析（服务质量分析、可观察性）等透明化增量功能；

### 可插拔

项目采用微内核 + 三层可插拔模型，使内核、功能组件以及生态对接完全能够灵活的方式进行插拔式扩展，开发者能够像使用积木一样定制属于自己的独特系统。

## 运行模式

Apache ShardingSphere 是一套完善的产品，使用场景非常广泛。 除生产环境的集群部署之外，还为工程师在开发和自动化测试等场景提供相应的运行模式。 Apache ShardingSphere 提供的 3 种运行模式分别是内存模式、单机模式和集群模式。
源码：https://github.com/apache/shardingsphere/tree/master/shardingsphere-mode

### 内存模式

初始化配置或执行 SQL 等造成的元数据结果变更的操作，仅在当前进程中生效。 适用于集成测试的环境启动，方便开发人员在整合功能测试中集成 Apache ShardingSphere 而无需清理运行痕迹。

### 单机模式

能够将数据源和规则等元数据信息持久化，但无法将元数据同步至多个 Apache ShardingSphere 实例，无法在集群环境中相互感知。 通过某一实例更新元数据之后，会导致其他实例由于获取不到最新的元数据而产生不一致的错误。 适用于工程师在本地搭建 Apache ShardingSphere 环境。

### 集群模式

提供了多个 Apache ShardingSphere 实例之间的元数据共享和分布式场景下状态协调的能力。 在真实部署上线的生产环境，必须使用集群模式。它能够提供计算能力水平扩展和高可用等分布式系统必备的能力。 集群环境需要通过独立部署的注册中心来存储元数据和协调节点状态。
