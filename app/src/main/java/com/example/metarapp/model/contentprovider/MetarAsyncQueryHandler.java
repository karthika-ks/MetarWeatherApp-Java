package com.example.metarapp.model.contentprovider;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.lang.ref.WeakReference;

public class MetarAsyncQueryHandler extends AsyncQueryHandler {
    private final WeakReference<DatabaseManager> mDataManagerRef;

    public MetarAsyncQueryHandler(DatabaseManager dataManager, ContentResolver cr) {
        super(cr);
        mDataManagerRef = new WeakReference<>(dataManager);
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        super.onQueryComplete(token, cookie, cursor);

        DatabaseManager dataManager = mDataManagerRef.get();
        if (dataManager != null) {
            dataManager.onQueryComplete(token, cursor);
        }
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        super.onInsertComplete(token, cookie, uri);

        DatabaseManager dataManager = mDataManagerRef.get();
        if (dataManager != null) {
            dataManager.onInsertComplete();
        }
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        super.onUpdateComplete(token, cookie, result);

        DatabaseManager dataManager = mDataManagerRef.get();
        if (dataManager != null) {
            dataManager.onUpdateComplete();
        }
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        super.onDeleteComplete(token, cookie, result);

        DatabaseManager dataManager = mDataManagerRef.get();
        if (dataManager != null) {
            dataManager.onDeleteComplete();
        }
    }
}
