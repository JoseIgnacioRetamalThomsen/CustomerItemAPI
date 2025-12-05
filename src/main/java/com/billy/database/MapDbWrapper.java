package com.billy.database;

import org.mapdb.DB;
import org.mapdb.DBMaker;

public class MapDbWrapper implements AutoCloseable {
    private final DB db;

    public MapDbWrapper(String file) {
        this.db = DBMaker
                .fileDB(file)
                .transactionEnable()
                .fileMmapEnableIfSupported()
                .checksumHeaderBypass()
                .closeOnJvmShutdown()
                .make();
    }

    public DB db() {
        return db;
    }

    public void commit() {
        db.commit();
    }

    public void rollback() {
        db.rollback();
    }

    @Override
    public void close() {
        db.close();
    }
}
