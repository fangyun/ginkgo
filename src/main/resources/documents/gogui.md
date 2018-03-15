# gogui

gogui是个Java的应用程序。

## 1. 附加到一个围棋引擎程序进程

利用java.lang.Process类构造出目标围棋引擎程序的进程。这个进程是任意可执行程序的进程，不限定为java应用。

通过Process类的API得到这个进程的输入流和输出流，缺省是标准输入流与标准输出流。遵循GTP协议，gogui向输入流提交命令，从输出流中获取引擎的响应。

## 2. 发送命令name

参考：[Ginkgo#1](Ginkgo.html) 和 [Ginkgo#1.1](Ginkgo.html)

## 3. 发送命令protocol_version

参考：[Ginkgo#1](Ginkgo.html) 和 [Ginkgo#1.2](Ginkgo.html)

## 4. 发送命令version

参考：[Ginkgo#1](Ginkgo.html) 和 [Ginkgo#1.3](Ginkgo.html)