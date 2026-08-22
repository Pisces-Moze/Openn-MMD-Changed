package com.shiroha.mmdskin;

import com.shiroha.mmdskin.render.bootstrap.ClientRenderRuntime;
import com.shiroha.mmdskin.util.VectorParseUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public class MmdSkinClient {
    public static final Logger logger = LogManager.getLogger();

    public static void initClient() {
        // OpenMMDChanged owns fixed model installation and intentionally does not
        // start MMDSkin's stage, model-selector, key-binding, or download flows.
        ClientRenderRuntime.initialize();
    }

    public static String calledFrom(int i){
        StackTraceElement[] steArray = Thread.currentThread().getStackTrace();
        if (steArray.length <= i) {
            return "";
        }
        return steArray[i].getClassName();
    }

    public static Vector3f str2Vec3f(String arg){
        return VectorParseUtil.parseVec3f(arg);
    }

}
