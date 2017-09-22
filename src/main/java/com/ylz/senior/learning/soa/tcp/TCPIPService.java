package com.ylz.senior.learning.soa.tcp;

import com.ylz.senior.learning.soa.interfaces.ISayHelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jonas on 2017/8/23.
 */
public class TCPIPService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TCPIPService.class);
    private static Map<String, Object> services = new HashMap<String, Object>();

    static {
        services.put(ISayHelloService.class.getName(), ISayHelloService.class);
    }

    public static void main(String[] args) {
        try {
            final int port = 1234;
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                String interfaceName = inputStream.readUTF();
                String methodName = inputStream.readUTF();
                Class<?>[] paramsTypes = (Class<?>[]) inputStream.readObject();
                Object[] params = (Object[]) inputStream.readObject();
                Class serviceInterfaceClass = Class.forName(interfaceName);
                Object service = services.get(interfaceName);
                Method method = serviceInterfaceClass.getMethod(methodName, paramsTypes);
                Object result = method.invoke(service, params);
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(result);
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        } catch (InvocationTargetException e) {
            LOGGER.error("", e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("", e);
        }


    }
}
