import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        bio(12301);
        nio(12302);
        aio(12303);
        // And then, you can use telnet to send message to those posts.
        // telnet 127.0.0.1 12301
    }

    public static void bio(int port) {
        Runnable bioRead = new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    // 等待 資料寫道從介面寫到快取
                    Socket socket = serverSocket.accept();
                    // 從快取中讀出來
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    bufferedReader.lines().forEach(System.out::println);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread bioReadThread = new Thread(bioRead);
        bioReadThread.start();
    }

    // 這份扣有太多模凌兩可的物件，導致變得很難解釋
    // 建議使用下面的扣
    public static void nio(int port) {
        Runnable mioRead = new Runnable() {
            // 執行緒要執行的內容
            @Override
            public void run() {
                // ServerSocketChannel : 用來接受新的connection
                // ServerSocket : 服務
                // SocketChannel : 可以想像是一個connection
                // Java : 物件自嗨大師

                try {
                    // Selector是Java NIO中的一个组件，
                    // https://wiki.jikexueyuan.com/project/java-nio-zh/java-nio-selector.html
                    // 首先是建立一個選擇器
                    Selector selector = Selector.open();

                    // 打開一個server socket channel，並設定為非阻塞
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);

                    // 打開服務器通道
                    ServerSocket serverSocket = serverSocketChannel.socket();
                    serverSocket.bind(new InetSocketAddress(port));
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                    while (true) {
                        // 返回的整數值通知有多少個通道準備好進行通信
                        int noOfKeys = selector.select();

                        // 取得準備就緒的通道
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();

                        // 可以解釋成把set轉換成array的形式方便逐一讀取
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();

                        // 如果可以讀取下一個可以讀去
                        while (iterator.hasNext()) {
                            // 讀取下一個
                            SelectionKey key = iterator.next();

                            // 判斷狀態
                            if (key.isAcceptable()) {
                                // 
                                // The new client connection is accepted
                                ServerSocketChannel scc = (ServerSocketChannel) key.channel();
                                SocketChannel socketChannel = scc.accept();
                                socketChannel.configureBlocking(false);

                                 // The new connection is added to a selector
                                socketChannel.register(selector, SelectionKey.OP_READ);
                            } else if (key.isReadable()) {
                                // 準備讀取
                                SocketChannel socketChannel = (SocketChannel) key.channel();

                                // 後面就不解釋了，把資料從快取複製過來
                                while (true) {
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                    if (socketChannel.read(byteBuffer) <= 0) {
                                        break;
                                    }
                                    byteBuffer.flip();
                                    Charset charset = StandardCharsets.UTF_8;
                                    System.out.print(charset.newDecoder().decode(byteBuffer).toString());
                                }
                            }

                            // 為了讓hasNext可以讀到下一個
                            iterator.remove();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread mioReadThread = new Thread(mioRead);
        mioReadThread.start();
    }

    // 來源
    // https://www.1ju.org/java-nio/java-nio-selector
    // 還有搭配圖片使用
    // https://www.google.com/search?q=network%20socket%20programming&tbm=isch&ved=2ahUKEwiA-7r91-DyAhUSx2EKHeb1C9cQ2-cCegQIABAC&oq=network%20socket%20programming&gs_lcp=ChJtb2JpbGUtZ3dzLXdpei1pbWcQAzIECAAQEzIECAAQEzIFCAAQzQIyBQgAEM0CMggIABAIEB4QEzoHCCMQ7wMQJzoGCAAQBxAeOgQIABAeOggIABAIEAcQHjoKCAAQCBAHEB4QE1C4HligK2DlMGgAcAB4AIABaYgBuwWSAQM3LjGYAQCgAQHAAQE&sclient=mobile-gws-wiz-img&ei=wPgwYYCbPJKOhwPm66-4DQ&bih=727&biw=412&client=ms-android-hmd-rev2&prmd=ivbxn&fbclid=IwAR2iyvX_PH1x7Usiu0jm6qI0YzHRY8PYMoGu4iY2oTJLXy3V9Kw3SkKpMNU#imgrc=RDBNi2JcQ-0pCM
    public static void nioEx(int port) throws IOException {
        // Get the selector
        Selector selector = Selector.open();
        System.out.println("Selector is open for making connection: " + selector.isOpen());

        // Get the server socket channel and register using selector
        ServerSocketChannel SS = ServerSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8080);
        SS.bind(hostAddress);
        SS.configureBlocking(false);
        int ops = SS.validOps();
        SelectionKey selectKy = SS.register(selector, ops, null);


        while (true) {
            // 這邊需要等待 selector 回傳結果
            System.out.println("Waiting for the select operation...");

            // 返回的整數值通知有多少個通道準備好進行通信
            int noOfKeys = selector.select();
            System.out.println("The Number of selected keys are: " + noOfKeys);

            // 可以解釋成把set轉換成array的形式方便逐一讀取
            Set selectedKeys = selector.selectedKeys();
            Iterator itr = selectedKeys.iterator();

            while (itr.hasNext()) {
                //讀取下一個
                SelectionKey ky = (SelectionKey) itr.next();

                // 取得狀態
                if (ky.isAcceptable()) {
                    // The new client connection is accepted
                    SocketChannel client = SS.accept();
                    client.configureBlocking(false);
                    // The new connection is added to a selector
                    // 新連線已建立
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("The new connection is accepted from the client: " + client);
                } else if (ky.isReadable()) {
                    // Data is read from the client
                    SocketChannel client = (SocketChannel) ky.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(256);
                    client.read(buffer);
                    String output = new String(buffer.array()).trim();
                    System.out.println("Message read from client: " + output);
                }

                // 為了讓hasNext可以讀到下一個
                itr.remove();
            } // end of while loop
        } // end of for loop

        /*
        Selector is open for making connection: true
        Waiting for the select operation...
        The Number of selected keys are: 1
        The new connection is accepted from the client: java.nio.channels.SocketChannel[connected local=/127.0.0.1:8080 remote=/127.0.0.1:53823]
        Waiting for the select operation...
        The Number of selected keys are: 1
        Message read from client: Time goes fast.
        Waiting for the select operation...
        The Number of selected keys are: 1
        Message read from client: What next?
        Waiting for the select operation...
        The Number of selected keys are: 1
        Message read from client: Bye Bye
        The Client messages are complete; close the session.
        */
    }

    public static void aio(int port) {
        Runnable aioRead = new Runnable() {
            @Override
            public void run() {
                try {
                    // 開一個 非同步的 server socket channel
                    AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
                    serverSocketChannel.bind(new InetSocketAddress(port));

                    CompletionHandler<AsynchronousSocketChannel, Object> handler = new CompletionHandler<AsynchronousSocketChannel, Object>() {
                        @Override
                        public void completed(final AsynchronousSocketChannel result, final Object attachment) {
                            // 直接收到讀取結果
                            serverSocketChannel.accept(attachment, this);
                            try {
                                // 把結過讀取出來
                                while (true) {
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                    if (result.read(byteBuffer).get() < 0) {
                                        break;
                                    }
                                    byteBuffer.flip();
                                    Charset charset = StandardCharsets.UTF_8;
                                    System.out.print(charset.newDecoder().decode(byteBuffer).toString());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void failed(final Throwable exc, final Object attachment) {
                            System.out.println("ERROR" + exc.getMessage());
                        }
                    };
                    serverSocketChannel.accept(null, handler);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        Thread aioReadThread = new Thread(aioRead);
        aioReadThread.start();
    }
}

