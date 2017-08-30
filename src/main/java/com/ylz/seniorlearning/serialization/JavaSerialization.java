package com.ylz.seniorlearning.serialization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Java serialization is easily to use, but it used too much system resource and slow.
 */
public class JavaSerialization<T> {
    private final Logger logger = LoggerFactory.getLogger(JavaSerialization.class);

    private void serializedObject(T t) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(os);
            oos.writeObject(t);
        } catch (IOException e) {
            logger.error("object : {}  serialized fail", t, e);
        }
    }

    private T unSerializedObject() {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] tBytes = os.toByteArray();
            ByteArrayInputStream is = new ByteArrayInputStream(tBytes);
            ObjectInputStream in = new ObjectInputStream(is);
            return (T) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
