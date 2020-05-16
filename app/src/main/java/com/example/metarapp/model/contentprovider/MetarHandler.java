package com.example.metarapp.model.contentprovider;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.metarapp.viewmodel.MetarViewModel;

import java.lang.ref.WeakReference;

public class MetarHandler extends AsyncQueryHandler {
    private final WeakReference<MetarViewModel> mViewModelRef;

    public MetarHandler(MetarViewModel viewModel, ContentResolver cr) {
        super(cr);
        mViewModelRef = new WeakReference<>(viewModel);
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        super.onQueryComplete(token, cookie, cursor);

        MetarViewModel viewModel = mViewModelRef.get();
        if (viewModel != null) {
            viewModel.onQueryComplete(cursor);
        }
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        super.onInsertComplete(token, cookie, uri);
        Log.i("MetarHandler", "onInsertComplete: ");

        MetarViewModel viewModel = mViewModelRef.get();
        if (viewModel != null) {
            viewModel.onInsertComplete();
        }
    }

    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        super.onUpdateComplete(token, cookie, result);

        MetarViewModel viewModel = mViewModelRef.get();
        if (viewModel != null) {
            viewModel.onUpdateComplete();
        }
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        super.onDeleteComplete(token, cookie, result);

        MetarViewModel viewModel = mViewModelRef.get();
        if (viewModel != null) {
            viewModel.onDeleteComplete();
        }
    }
}
