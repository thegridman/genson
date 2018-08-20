package com.owlike.genson.convert;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import com.owlike.genson.Context;
import com.owlike.genson.Converter;
import com.owlike.genson.Genson;
import com.owlike.genson.JsonBindingException;
import com.owlike.genson.Wrapper;
import com.owlike.genson.reflect.TypeUtil;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;

/**
 * This converter will use the runtime type of objects during serialization.
 *
 * @param <T> the type this converter is handling.
 * @author eugen
 */
public class RuntimeTypeConverter<T> extends Wrapper<Converter<T>> implements Converter<T> {

  private static final String CYCLE_KEY = "cycle-detection";

    public static class RuntimeTypeConverterFactory extends ChainedFactory {
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        protected Converter<?> create(Type type, Genson genson, Converter<?> nextConverter) {
            if (nextConverter == null)
                throw new IllegalArgumentException(
                        "RuntimeTypeConverter can not be last Converter in the chain.");
            return (Converter<?>) new RuntimeTypeConverter(TypeUtil.getRawClass(type),
                    nextConverter);
        }
    }

    private final Class<T> tClass;

    @SuppressWarnings("unchecked")
    public RuntimeTypeConverter(Class<T> tClass, Converter<T> next) {
        super(next);
        this.tClass = tClass.isPrimitive()
                ? (Class<T>) TypeUtil.wrap(tClass)
                : tClass;
    }

    public void serialize(T obj, ObjectWriter writer, Context ctx) throws Exception {
        if (obj != null
            && !tClass.equals(obj.getClass())
            && !isContainer(obj)) {
          ensureNoCircularRefs(obj, ctx);
          ctx.genson.serialize(obj, obj.getClass(), writer, ctx);
          clearCircularCheckRefs(obj, ctx);
        } else {
          wrapped.serialize(obj, writer, ctx);
        }
    }

    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
        return wrapped.deserialize(reader, ctx);
    }

    private boolean isContainer(T obj) {
        return obj.getClass().isArray() ||
                obj instanceof Collection ||
                obj instanceof Map;
    }

    private boolean isSimpleType(T obj) {
      return obj instanceof Boolean ||
          obj instanceof Number ||
          obj instanceof String;
    }

  private void ensureNoCircularRefs(T obj, Context ctx) {
    if (!isSimpleType(obj)) {
      Map seen = ctx.get(CYCLE_KEY, Map.class);
      if (seen == null) {
        seen = new IdentityHashMap<>();
        ctx.store(CYCLE_KEY, seen);
      }
      if (seen.put(obj, Boolean.TRUE) != null) {
        throw new JsonBindingException("Cyclic object graphs are not supported.");
      }
    }
  }

  private void clearCircularCheckRefs(T obj, Context ctx) {
    Map seen = ctx.get(CYCLE_KEY, Map.class);
    if (seen != null) {
      seen.remove(obj);
    }
  }


}
