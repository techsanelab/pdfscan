package com.techsanelab.pdfscan.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.airbnb.lottie.LottieAnimationView;
import com.dd.morphingbutton.MorphingButton;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.techsanelab.pdfscan.R;
import com.techsanelab.pdfscan.adapter.FilesListAdapter;
import com.techsanelab.pdfscan.adapter.MergeFilesAdapter;
import com.techsanelab.pdfscan.database.DatabaseHelper;
import com.techsanelab.pdfscan.interfaces.BottomSheetPopulate;
import com.techsanelab.pdfscan.interfaces.OnBackPressedInterface;
import com.techsanelab.pdfscan.interfaces.OnPDFCreatedInterface;
import com.techsanelab.pdfscan.util.BottomSheetCallback;
import com.techsanelab.pdfscan.util.BottomSheetUtils;
import com.techsanelab.pdfscan.util.CommonCodeUtils;
import com.techsanelab.pdfscan.util.DialogUtils;
import com.techsanelab.pdfscan.util.FileUtils;
import com.techsanelab.pdfscan.util.MorphButtonUtility;
import com.techsanelab.pdfscan.util.PermissionsUtils;
import com.techsanelab.pdfscan.util.RealPathUtil;
import com.techsanelab.pdfscan.util.RemoveDuplicates;
import com.techsanelab.pdfscan.util.StringUtils;

import static android.app.Activity.RESULT_OK;
import static com.techsanelab.pdfscan.util.Constants.REQUEST_CODE_FOR_WRITE_PERMISSION;
import static com.techsanelab.pdfscan.util.Constants.WRITE_PERMISSIONS;

public class RemoveDuplicatePagesFragment extends Fragment implements MergeFilesAdapter.OnClickListener,
        FilesListAdapter.OnFileItemClickedListener, BottomSheetPopulate, OnPDFCreatedInterface, OnBackPressedInterface {

    private Activity mActivity;
    private String mPath;
    private MorphButtonUtility mMorphButtonUtility;
    private FileUtils mFileUtils;
    private BottomSheetUtils mBottomSheetUtils;
    private static final int INTENT_REQUEST_PICKFILE_CODE = 10;
    private MaterialDialog mMaterialDialog;
    BottomSheetBehavior mSheetBehavior;

    @BindView(R.id.lottie_progress)
    LottieAnimationView mLottieProgress;
    @BindView(R.id.selectFile)
    MorphingButton selectFileButton;
    @BindView(R.id.remove)
    MorphingButton removeDuplicateButton;
    @BindView(R.id.bottom_sheet)
    LinearLayout layoutBottomSheet;
    @BindView(R.id.upArrow)
    ImageView mUpArrow;
    @BindView(R.id.downArrow)
    ImageView mDownArrow;
    @BindView(R.id.layout)
    RelativeLayout mLayout;
    @BindView(R.id.recyclerViewFiles)
    RecyclerView mRecyclerViewFiles;
    @BindView(R.id.view_pdf)
    Button mViewPdf;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.fragment_remove_duplicate_pages, container, false);
        ButterKnife.bind(this, rootview);
        mSheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        mSheetBehavior.setBottomSheetCallback(new BottomSheetCallback(mUpArrow, isAdded()));
        mLottieProgress.setVisibility(View.VISIBLE);
        mBottomSheetUtils.populateBottomSheetWithPDFs(this);
        getRuntimePermissions();
        resetValues();
        return rootview;
    }
    @OnClick(R.id.viewFiles)
    void onViewFilesClick(View view) {
        mBottomSheetUtils.showHideSheet(mSheetBehavior);
    }

    /**
     * Displays file chooser intent
     */
    @OnClick(R.id.selectFile)
    public void showFileChooser() {
        startActivityForResult(mFileUtils.getFileChooser(),
                INTENT_REQUEST_PICKFILE_CODE);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) throws NullPointerException {
        if (data == null || resultCode != RESULT_OK || data.getData() == null)
            return;
        if (requestCode == INTENT_REQUEST_PICKFILE_CODE) {
            //Getting Absolute Path
            String path = RealPathUtil.getInstance().getRealPath(getContext(), data.getData());
            setTextAndActivateButtons(path);
        }
    }

    //On click remove duplicate button
    @OnClick(R.id.remove)
    public void parse() {
        new RemoveDuplicates(mPath, this).execute();
    }

    private void resetValues() {
        mPath = null;
        mMorphButtonUtility.initializeButton(selectFileButton, removeDuplicateButton);
    }

    private void setTextAndActivateButtons(String path) {
        mPath = path;
        mMorphButtonUtility.setTextAndActivateButtons(path,
                selectFileButton, removeDuplicateButton);
    }

    @Override
    public void onPopulate(ArrayList<String> paths) {
        CommonCodeUtils.getInstance().populateUtil(mActivity, paths,
                this, mLayout, mLottieProgress, mRecyclerViewFiles);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mFileUtils = new FileUtils(mActivity);
        mBottomSheetUtils = new BottomSheetUtils(mActivity);
    }

    @Override
    public void onItemClick(String path) {
        mSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        setTextAndActivateButtons(path);
    }

    @Override
    public void onFileItemClick(String path) {
        mFileUtils.openFile(path, FileUtils.FileType.e_PDF);
    }

    private void viewPdfButton(String path) {
        mViewPdf.setVisibility(View.VISIBLE);
        mViewPdf.setOnClickListener(v -> mFileUtils.openFile(path, FileUtils.FileType.e_PDF));
    }

    @Override
    public void onPDFCreationStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(mActivity);
        mMaterialDialog.show();
    }

    @Override
    public void onPDFCreated(boolean isNewPdfCreated, String path) {
        mMaterialDialog.dismiss();
        if (!isNewPdfCreated) {
            StringUtils.getInstance().showSnackbar(mActivity, R.string.snackbar_no_duplicate_pdf);
            //Hiding View PDF button
            mViewPdf.setVisibility(View.GONE);
            return;
        }
        new DatabaseHelper(mActivity).insertRecord(path, mActivity.getString(R.string.created));
        StringUtils.getInstance().getSnackbarwithAction(mActivity, R.string.snackbar_duplicate_removed)
                .setAction(R.string.snackbar_viewAction,
                        v -> mFileUtils.openFile(path, FileUtils.FileType.e_PDF)).show();
        viewPdfButton(path);
        resetValues();
    }

    @Override
    public void closeBottomSheet() {
        CommonCodeUtils.getInstance().closeBottomSheetUtil(mSheetBehavior);
    }

    @Override
    public boolean checkSheetBehaviour() {
        return CommonCodeUtils.getInstance().checkSheetBehaviourUtil(mSheetBehavior);
    }

    /***
     * check runtime permissions for storage and camera
     ***/
    private void getRuntimePermissions() {
        if (Build.VERSION.SDK_INT < 29) {
            PermissionsUtils.getInstance().requestRuntimePermissions(this,
                    WRITE_PERMISSIONS,
                    REQUEST_CODE_FOR_WRITE_PERMISSION);
        }
    }
}