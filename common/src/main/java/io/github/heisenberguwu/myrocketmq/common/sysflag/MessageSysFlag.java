package io.github.heisenberguwu.myrocketmq.common.sysflag;

import io.github.heisenberguwu.myrocketmq.common.compression.CompressionType;

public class MessageSysFlag {

    /**
     * Meaning of each bit in the system flag
     *
     * | bit    | 7 | 6 | 5         | 4        | 3           | 2                | 1                | 0                |
     * |--------|---|---|-----------|----------|-------------|------------------|------------------|------------------|
     * | byte 1 |   |   | STOREHOST | BORNHOST | TRANSACTION | TRANSACTION      | MULTI_TAGS       | COMPRESSED       |
     * | byte 2 |   |   |           |          |             | COMPRESSION_TYPE | COMPRESSION_TYPE | COMPRESSION_TYPE |
     * | byte 3 |   |   |           |          |             |                  |                  |                  |
     * | byte 4 |   |   |           |          |             |                  |                  |                  |
     */
    public final static int COMPRESSED_FLAG = 0x1;
    public final static int MULTI_TAGS_FLAG = 0x1 << 1;
    public final static int TRANSACTION_NOT_TYPE = 0;
    public final static int TRANSACTION_PREPARED_TYPE = 0x1 << 2;
    public final static int TRANSACTION_COMMIT_TYPE = 0x2 << 2;
    public final static int TRANSACTION_ROLLBACK_TYPE = 0x3 << 2;
    public final static int BORNHOST_V6_FLAG = 0x1 << 4;
    public final static int STOREHOSTADDRESS_V6_FLAG = 0x1 << 5;
    //Mark the flag for batch to avoid conflict
    public final static int NEED_UNWRAP_FLAG = 0x1 << 6;
    public final static int INNER_BATCH_FLAG = 0x1 << 7;

    // COMPRESSION_TYPE
    public final static int COMPRESSION_LZ4_TYPE = 0x1 << 8;
    public final static int COMPRESSION_ZSTD_TYPE = 0x2 << 8;
    public final static int COMPRESSION_ZLIB_TYPE = 0x3 << 8;
    public final static int COMPRESSION_TYPE_COMPARATOR = 0x7 << 8;

    public static int getTransactionValue(final int flag) {
        return flag & TRANSACTION_ROLLBACK_TYPE;
    }

    public static int resetTransactionValue(final int flag, final int type) {
        return (flag & (~TRANSACTION_ROLLBACK_TYPE)) | type;
    }

    public static int clearCompressedFlag(final int flag) {
        return flag & (~COMPRESSED_FLAG);
    }

    // To match the compression type
    public static CompressionType getCompressionType(final int flag) {
        return CompressionType.findByValue((flag & COMPRESSION_TYPE_COMPARATOR) >> 8);
    }

    public static boolean check(int flag, int expectedFlag) {
        return (flag & expectedFlag) != 0;
    }

}
