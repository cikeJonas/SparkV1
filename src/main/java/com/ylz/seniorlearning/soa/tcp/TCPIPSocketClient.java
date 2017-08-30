package com.ylz.seniorlearning.soa.tcp;

import com.ylz.seniorlearning.soa.interfaces.ISayHelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Jonas on 2017/8/23.
 */
public class TCPIPSocketClient {
    private final Logger log = LoggerFactory.getLogger(TCPIPSocketClient.class);

    public static void main(String[] args) {
        TCPIPSocketClient tcpipSocket = new TCPIPSocketClient();
        System.out.println(tcpipSocket.service());
    }

    public String service() {
        String interfaceName = ISayHelloService.class.getName();
        try {
            final int port = 1234;
            Method method = ISayHelloService.class.getMethod("sayHello", String.class);
            Object[] args = {"hello"};
            Socket socket = new Socket("127.0.0.1", port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeUTF(interfaceName);
            outputStream.writeUTF(method.getName());
            outputStream.writeObject(method.getParameterTypes());
            outputStream.writeObject(args);

            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            return inputStream.readObject().toString();
        } catch (NoSuchMethodException e) {
            log.error("", e);
        } catch (UnknownHostException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        } catch (ClassNotFoundException e) {
            log.error("", e);
        }
        return null;
    }
}
