# Ginkgo.java

## 1. 处理命令

接受到命令，通过使用if/else if/else的条件分支语句，分配到不同的语句块中进行相关处理。

### 1.1 处理命令name的逻辑

简单地向标准输出流打印字符串：Ginkgo的名称

### 1.2 处理命令protocol_version的逻辑

简单地向标准输出流打印字符串：2

### 1.3 处理命令version的逻辑

* java.lang.ProcessBuilder.ProcessBuilder调用git命令：
```bash
git --git-dir=$WorkDir/.git --work-tree=$WorkDir log --pretty=format:'%H' -n 1
```

* 获取上面进程的输入流，传给java.util.Scanner
* 从Scanner中获取下一行的字符串，格式化此字符串，向标准输出流打印。

