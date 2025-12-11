package com.tonic.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

public class ByteBufferPool
{
    private static final ByteBufAllocator ALLOCATOR = PooledByteBufAllocator.DEFAULT;
    private static final int BUFFER_SIZE = 200;

    public static ByteBuf allocate() {
        return ALLOCATOR.buffer(BUFFER_SIZE);
    }

    public static ByteBuf allocate(int size) {
        return ALLOCATOR.buffer(size);
    }

    public static void release(ByteBuf buffer) {
        if (buffer != null) {
            buffer.release();
        }
    }
}