# Ginkgo.java

## 1. main方法

* 构造Ginkgo实例
* 调用Ginkgo实例的run方法
* 在run方法中处理命令。参考2.

### 1.1 构造Ginkgo实例

* 设置输入流
* 设置输出流
* 处理启动参数
  * 构建PlayerBuilder实例
  * 根据启动参数设置PlayerBuilder实例的状态
  * 由PlayerBuilder实例构建成员变量Player实例player。
* 初始化成员变量commands

## 2. 处理命令

从标准输入接受到命令，通过使用if/else if/else的条件分支语句，分配到不同的语句块中进行相关处理。

### 2.1 处理命令name

简单地向标准输出流打印字符串：Ginkgo的名称

### 2.2 处理命令protocol_version

简单地向标准输出流打印字符串：2

### 2.3 处理命令version

* java.lang.ProcessBuilder.ProcessBuilder调用git命令：
```bash
git --git-dir=$WorkDir/.git --work-tree=$WorkDir log --pretty=format:'%H' -n 1
```

* 获取上面进程的输入流，传给java.util.Scanner

* 从Scanner中获取下一行的字符串，格式化此字符串，向标准输出流打印。

### 2.4 处理命令list_commands

* 获取commands变量中存在的所有已知的GTP命令，拼接起来成多行字符串

* 向标准输出流打印此字符串。

### 2.5 处理命令gogui-analyze_commands

* 直接向标准输出流打印gfx为前缀的多条多行的分析命令字符串。

### 2.6 处理命令boardsize 19 

* 由player获取board，由board获取棋盘坐标系统coords
* 根据坐标系统coords查询其board的宽度width
* 如果width和boardsize命令的参数一致（这里是19），就仅仅player.clear()清洗下。
* 否则，设置playerBuilder的状态boardWidth=新的值后，重新构建player
* 应答空串

### 2.7 处理命令clear_board

- 调用[player](Player.html).clear()清洗下
- 应答空串

### 2.8 处理命令komi 6.5

* 如果和已有的贴目值一致，则调用[player](Player.html).clear()清洗下
* 否则，设置playerBuilder的状态komi为新值，重新构建player
* 应答空串