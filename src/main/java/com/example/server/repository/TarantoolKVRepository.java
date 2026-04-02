package com.example.server.repository;

import com.example.server.dto.KVEntryFound;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import io.tarantool.client.crud.TarantoolCrudClient;
import io.tarantool.mapping.TarantoolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class TarantoolKVRepository implements KVRepository {

    private final TarantoolCrudClient client;

    @Override
    public void put(String key, byte[] value) {
        List<Object> tuple = new ArrayList<>();
        tuple.add(key);
        tuple.add(value);

        client.call("crud.replace", List.of("KV", tuple)).join();
    }

    @Override
    public KVEntryFound get(String key) {
        Object response = client.call("crud.get", List.of("KV", key)).join();

        if (response instanceof TarantoolResponse<?> tr) {
            List<?> tuples = (List<?>) tr.get();

            if (!tuples.isEmpty()) {
                Object tupleObj = tuples.get(0);

                if (tupleObj instanceof List<?> outerList && !outerList.isEmpty()) {
                    Object innerObj = outerList.get(0);

                    if (innerObj instanceof List<?> innerList && innerList.size() == 2) {
                        Object valueObj = innerList.get(1);

                        if (valueObj == null) {
                            return new KVEntryFound(true, null);
                        }

                        byte[] bytes;

                        if (valueObj instanceof byte[] b) {
                            bytes = b;
                        } else {
                            throw new IllegalStateException("Unexpected value type: " + valueObj.getClass());
                        }

                        return new KVEntryFound(true,
                                BytesValue.newBuilder()
                                        .setValue(ByteString.copyFrom(bytes))
                                        .build());
                    }
                }
            }
        }

        return new KVEntryFound(false, null);
    }

    @Override
    public void delete(String key) {
        client.call("crud.delete", List.of("KV", key)).join();
    }

    @Override
    public long count() {
        Object response = client.call("crud.count", List.of("KV")).join();

        if (response instanceof TarantoolResponse<?> tr) {
            List<?> tuples = (List<?>) tr.get();

            if (!tuples.isEmpty()) {
                Object firstTuple = tuples.get(0);

                if (firstTuple instanceof List<?> inner && !inner.isEmpty()) {
                    Object value = inner.get(0);

                    if (value instanceof Number n) {
                        return n.longValue();
                    } else {
                        return Long.parseLong(value.toString());
                    }
                }
            }
        }

        return 0L;
    }

    public void streamRange(String keySince, String keyTo, RangeConsumer consumer) {
        int batchSize = 1000;
        String lastKey = null;

        while (true) {
            List<Object> conditions = new ArrayList<>();
            conditions.add(Arrays.asList("key", ">=", keySince));

            if (lastKey != null) {
                conditions.add(Arrays.asList("key", ">", lastKey));
            }

            if (keyTo != null && !keyTo.isEmpty()) {
                conditions.add(Arrays.asList("key", "<", keyTo));
            }

            Map<String, Object> opts = new HashMap<>();
            opts.put("first", batchSize);

            Object response = client.call("crud.select", List.of("KV", conditions, opts)).join();

            if (!(response instanceof TarantoolResponse<?> tr)) {
                throw new IllegalStateException("Unexpected response type: " + response.getClass());
            }

            List<?> outer = (List<?>) tr.get();

            if (outer.isEmpty()) break;

            Object tupleObj = outer.get(0);

            if (!(tupleObj instanceof List<?> tupleList) || tupleList.isEmpty()) break;

            Object resultObj = tupleList.get(0);

            if (!(resultObj instanceof Map<?, ?> resultMap)) {
                throw new IllegalStateException("Unexpected type: " + resultObj.getClass());
            }

            List<?> rows = (List<?>) resultMap.get("rows");

            if (rows == null || rows.isEmpty()) break;

            for (Object rowObj : rows) {
                if (!(rowObj instanceof List<?> row) || row.isEmpty()) {
                    continue;
                }

                String k = (String) row.get(0);
                Object valueObj = row.size() > 1 ? row.get(1) : null;

                byte[] bytes = null;

                if (valueObj != null) {
                    if (valueObj instanceof byte[] b) {
                        bytes = b;
                    } else {
                        throw new IllegalStateException("Unexpected value type: " + valueObj.getClass());
                    }
                }

                BytesValue valueProto = (bytes != null)
                        ? BytesValue.newBuilder().setValue(ByteString.copyFrom(bytes)).build()
                        : null;

                consumer.accept(k, valueProto);

                lastKey = k;
            }

            if (rows.size() < batchSize) break;
        }
    }
}
