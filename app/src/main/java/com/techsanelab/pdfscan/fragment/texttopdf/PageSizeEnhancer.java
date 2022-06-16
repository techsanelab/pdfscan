package com.techsanelab.pdfscan.fragment.texttopdf;

import android.content.Context;
import androidx.annotation.NonNull;

import com.techsanelab.pdfscan.R;
import com.techsanelab.pdfscan.interfaces.Enhancer;
import com.techsanelab.pdfscan.model.EnhancementOptionsEntity;
import com.techsanelab.pdfscan.preferences.TextToPdfPreferences;
import com.techsanelab.pdfscan.util.PageSizeUtils;

/**
 * An {@link Enhancer} that lets you select page size.
 */
public class PageSizeEnhancer implements Enhancer {

    private final PageSizeUtils mPageSizeUtils;
    private final EnhancementOptionsEntity mEnhancementOptionsEntity;

    PageSizeEnhancer(@NonNull final Context context) {
        mPageSizeUtils = new PageSizeUtils(context);
        mEnhancementOptionsEntity = new EnhancementOptionsEntity(
                context, R.drawable.ic_page_size_24dp, R.string.set_page_size_text);

        PageSizeUtils.mPageSize = new TextToPdfPreferences(context).getPageSize();
    }

    @Override
    public void enhance() {
        mPageSizeUtils.showPageSizeDialog(false);
    }

    @Override
    public EnhancementOptionsEntity getEnhancementOptionsEntity() {
        return mEnhancementOptionsEntity;
    }

}
