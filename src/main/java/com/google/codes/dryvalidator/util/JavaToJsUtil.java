package com.google.codes.dryvalidator.util;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.util.List;
import java.util.Map;

/**
 * @author kawasima
 */
public class JavaToJsUtil {
    public static NativeObject convert(Map map, Context ctx, Scriptable scope) {
        NativeObject obj = NativeObject.class.cast(ctx.newObject(scope));
        for (Object key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {
                value = JavaToJsUtil.convert((Map) value, ctx, scope);
            } else if (value instanceof List) {
                value = JavaToJsUtil.convert((List) value, ctx, scope);
            }
            obj.put(key.toString(), obj, value);
        }
        return obj;
    }

    public static NativeArray convert(List list, Context ctx, Scriptable scope) {
        NativeArray arr = NativeArray.class.cast(ctx.newArray(scope, 0));

        for (int i = 0; i < list.size(); i++) {
            Object el = list.get(i);
            if (el instanceof Map) {
                el = JavaToJsUtil.convert((Map) el, ctx, scope);
            } else if (el instanceof List) {
                el = JavaToJsUtil.convert((List) el, ctx, scope);
            }
            arr.put(i, arr, el);
        }
        return arr;
    }
}
