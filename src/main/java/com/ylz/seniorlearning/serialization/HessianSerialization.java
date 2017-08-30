package com.ylz.seniorlearning.serialization;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian Serialization 效率稍低,对各种面向对象编程语言友好,性能稳定,比Java的高.
 */
public class HessianSerialization<T> {
    private final Logger logger = LoggerFactory.getLogger(JavaSerialization.class);

    private ByteArrayInputStream serializedObject(T t) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            HessianOutput ho = new HessianOutput(os);
            ho.writeObject(t);
            byte[] tBytes = os.toByteArray();
            ByteArrayInputStream in = new ByteArrayInputStream(tBytes);
            return in;
        } catch (IOException e) {
            logger.error("object : {}  serialized fail", t, e);
        }
        return null;
    }

    private T unSerializedObject(ByteArrayInputStream is) {
        try {
            HessianInput hi = new HessianInput(is);
            return (T) hi.readObject();
        } catch (IOException e) {
            logger.error("Cann't unseriaized object: ", e);
            return null;
        }
    }
}
