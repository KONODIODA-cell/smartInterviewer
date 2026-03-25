package org.hane;

import sun.misc.Signal;

import java.time.Duration;
import java.util.Scanner;

public class UserInputBlockingTest {
  volatile static boolean running = true;
  volatile static boolean quitConfirm = false;
  volatile static boolean isBlocking = false;

  public static void main(String[] args) {
    // 重复键入 ctrl + c 退出功能
    // 在 JVM 关闭流程前拦截 SIGINT
    Signal.handle(new Signal("INT"), sig -> {
      if (quitConfirm) {
        // 第二次 Ctrl+C：真正退出
        System.out.println("\nquit...");
        System.exit(0);  // 主动退出，触发 Shutdown Hook
      } else {
        // 第一次 Ctrl+C：提示确认，不退出
        quitConfirm = true;
        System.out.println("\npress ctrl + c again to quit");
        System.out.print("> ");  // 重新显示提示符
      }
    });


    try (Scanner scanner = new Scanner(System.in)){
      while (running) {
        // 只在非阻塞状态下显示提示符并接收输入
        if (!isBlocking) {
          System.out.print("> ");
        }

        // 等待输入（hasNext 会阻塞）
        if (scanner.hasNext()) {
          String s = scanner.nextLine();

          // 关键：检查输入是否在阻塞期间产生的
          // 如果是，则丢弃并重新等待
          if (isBlocking) {
            continue;
          }

          Thread t = new Thread(new blockingMission(s));
          t.start();
          t.join();
        }
      }
    }catch (Exception e) {
      System.out.println("something went wrong: " + e.getMessage());
    }
  }

  static class spinner {
    // 旋转字符序列
    private static final char[] FRAMES = {'|', '/', '-', '\\'};
    private volatile boolean running = false;
    private Thread spinnerThread;

    // 启动旋转动画
    public void start() {
      if (running) return;
      running = true;

      spinnerThread = new Thread(() -> {
        int idx = 0;
        while (running) {
          // \r 回车：光标回到行首，覆盖上一帧
          System.out.print("\r" +FRAMES[idx % FRAMES.length]);
          System.out.flush();
          idx++;
          try {
            Thread.sleep(100);  // 控制帧率
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
        // 动画结束：回车 + 清空行 + 换行
        System.out.println("\r");
      }, "Spinner-Thread");
      spinnerThread.setDaemon(true);
      spinnerThread.start();
    }

    // 停止旋转动画
    public void stop() {
      running = false;
      if (spinnerThread != null) {
        try {
          spinnerThread.join(1000);  // 等待线程结束
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  static class blockingMission implements Runnable {
    private final String msg;

    public blockingMission(String msg) {
      this.msg = msg;
    }

    @Override
    public void run() {
      // 标记进入阻塞状态
      isBlocking = true;

      spinner spinner = new spinner();
      spinner.start();
      try {
        Thread.sleep(Duration.ofSeconds(10));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      spinner.stop();

      // 清除 spinner 最后一行
      System.out.print("\r\033[K");  // ANSI 转义码清除当前行
      System.out.println("user input: " + msg);

      // 关键：清空阻塞期间用户输入的所有内容
      try {
        while (System.in.available() > 0) {
          System.in.read();
        }
      } catch (Exception e) {
        // 忽略读取错误
      }

      // 标记阻塞结束
      isBlocking = false;
    }
  }
}