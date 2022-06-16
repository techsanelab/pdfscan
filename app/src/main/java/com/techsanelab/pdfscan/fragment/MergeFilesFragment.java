package com.techsanelab.pdfscan.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.airbnb.lottie.LottieAnimationView;
import com.dd.morphingbutton.MorphingButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.techsanelab.pdfscan.R;
import com.techsanelab.pdfscan.adapter.EnhancementOptionsAdapter;
import com.techsanelab.pdfscan.adapter.MergeFilesAdapter;
import com.techsanelab.pdfscan.adapter.MergeSelectedFilesAdapter;
import com.techsanelab.pdfscan.database.DatabaseHelper;
import com.techsanelab.pdfscan.interfaces.BottomSheetPopulate;
import com.techsanelab.pdfscan.interfaces.MergeFilesListener;
import com.techsanelab.pdfscan.interfaces.OnBackPressedInterface;
import com.techsanelab.pdfscan.interfaces.OnItemClickListener;
import com.techsanelab.pdfscan.model.EnhancementOptionsEntity;
import com.techsanelab.pdfscan.util.BottomSheetCallback;
import com.techsanelab.pdfscan.util.BottomSheetUtils;
import com.techsanelab.pdfscan.util.CommonCodeUtils;
import com.techsanelab.pdfscan.util.DefaultTextWatcher;
import com.techsanelab.pdfscan.util.DialogUtils;
import com.techsanelab.pdfscan.util.FileUtils;
import com.techsanelab.pdfscan.util.MergePdf;
import com.techsanelab.pdfscan.util.MergePdfEnhancementOptionsUtils;
import com.techsanelab.pdfscan.util.MorphButtonUtility;
import com.techsanelab.pdfscan.util.PermissionsUtils;
import com.techsanelab.pdfscan.util.RealPathUtil;
import com.techsanelab.pdfscan.util.StringUtils;
import com.techsanelab.pdfscan.util.ViewFilesDividerItemDecoration;

import static android.app.Activity.RESULT_OK;
import static com.techsanelab.pdfscan.util.Constants.MASTER_PWD_STRING;
import static com.techsanelab.pdfscan.util.Constants.REQUEST_CODE_FOR_WRITE_PERMISSION;
import static com.techsanelab.pdfscan.util.Constants.STORAGE_LOCATION;
import static com.techsanelab.pdfscan.util.Constants.WRITE_PERMISSIONS;
import static com.techsanelab.pdfscan.util.Constants.appName;

public class MergeFilesFragment extends Fragment implements MergeFilesAdapter.OnClickListener, MergeFilesListener,
        MergeSelectedFilesAdapter.OnFileItemClickListener, OnItemClickListener,
        BottomSheetPopulate, OnBackPressedInterface {
    private Activity mActivity;
    private String mCheckbtClickTag = "";
    private static final int INTENT_REQUEST_PICK_FILE_CODE = 10;
    private MorphButtonUtility mMorphButtonUtility;
    private ArrayList<String> mFilePaths;
    private FileUtils mFileUtils;
    private BottomSheetUtils mBottomSheetUtils;
    private MergeSelectedFilesAdapter mMergeSelectedFilesAdapter;
    private MaterialDialog mMaterialDialog;
    private String mHomePath;
    private ArrayList<EnhancementOptionsEntity> mEnhancementOptionsEntityArrayList;
    private EnhancementOptionsAdapter mEnhancementOptionsAdapter;
    private boolean mPasswordProtected = false;
    private String mPassword;
    private SharedPreferences mSharedPrefs;
    private BottomSheetBehavior mSheetBehavior;

    @BindView(R.id.lottie_progress)
    LottieAnimationView mLottieProgress;
    @BindView(R.id.mergebtn)
    MorphingButton mergeBtn;
    @BindView(R.id.recyclerViewFiles)
    RecyclerView mRecyclerViewFiles;
    @BindView(R.id.upArrow)
    ImageView mUpArrow;
    @BindView(R.id.downArrow)
    ImageView mDownArrow;
    @BindView(R.id.layout)
    RelativeLayout mLayout;
    @BindView(R.id.bottom_sheet)
    LinearLayout layoutBottomSheet;
    @BindView(R.id.selectFiles)
    Button mSelectFiles;
    @BindView(R.id.selected_files)
    RecyclerView mSelectedFiles;
    @BindView(R.id.enhancement_options_recycle_view)
    RecyclerView mEnhancementOptionsRecycleView;

    public MergeFilesFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_merge_files, container, false);
        ButterKnife.bind(this, root);
        showEnhancementOptions();
        mSheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        mFilePaths = new ArrayList<>();
        mMergeSelectedFilesAdapter = new MergeSelectedFilesAdapter(mActivity, mFilePaths, this);
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mHomePath = mSharedPrefs.getString(STORAGE_LOCATION,
                StringUtils.getInstance().getDefaultStorageLocation());
        mLottieProgress.setVisibility(View.VISIBLE);
        mBottomSheetUtils.populateBottomSheetWithPDFs(this);

        mSelectedFiles.setAdapter(mMergeSelectedFilesAdapter);
        mSelectedFiles.addItemDecoration(new ViewFilesDividerItemDecoration(mActivity));

        mSheetBehavior.setBottomSheetCallback(new BottomSheetCallback(mUpArrow, isAdded()));
        setMorphingButtonState(false);

        getRuntimePermissions();

        return root;
    }

    /**
     * Function to show the enhancement options.
     */
    private void showEnhancementOptions() {
        GridLayoutManager mGridLayoutManager = new GridLayoutManager(mActivity, 2);
        mEnhancementOptionsRecycleView.setLayoutManager(mGridLayoutManager);
        mEnhancementOptionsEntityArrayList = MergePdfEnhancementOptionsUtils.getInstance()
                .getEnhancementOptions(mActivity);
        mEnhancementOptionsAdapter = new EnhancementOptionsAdapter(this, mEnhancementOptionsEntityArrayList);
        mEnhancementOptionsRecycleView.setAdapter(mEnhancementOptionsAdapter);
    }

    @Override
    public void onItemClick(int position) {
        if (mFilePaths.size() == 0) {
            StringUtils.getInstance().showSnackbar(mActivity, R.string.snackbar_no_pdfs_selected);
            return;
        }
        if (position == 0) {
            setPassword();
        }
    }

    private void setPassword() {
        MaterialDialog.Builder builder = DialogUtils.getInstance().createCustomDialogWithoutContent(mActivity,
                R.string.set_password);
        final MaterialDialog dialog = builder
                .customView(R.layout.custom_dialog, true)
                .neutralText(R.string.remove_dialog)
                .build();

        final View positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        final View neutralAction = dialog.getActionButton(DialogAction.NEUTRAL);
        final EditText passwordInput = Objects.requireNonNull(dialog.getCustomView()).findViewById(R.id.password);
        passwordInput.setText(mPassword);
        passwordInput.addTextChangedListener(
                new DefaultTextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }

                    @Override
                    public void afterTextChanged(Editable input) {
                        if (StringUtils.getInstance().isEmpty(input)) {
                            StringUtils.getInstance().
                                    showSnackbar(mActivity, R.string.snackbar_password_cannot_be_blank);
                        } else {
                            mPassword = input.toString();
                            mPasswordProtected = true;
                            onPasswordStatusChanges(true);
                        }
                    }
                });
        if (StringUtils.getInstance().isNotEmpty(mPassword)) {
            neutralAction.setOnClickListener(v -> {
                mPassword = null;
                onPasswordStatusChanges(false);
                mPasswordProtected = false;
                dialog.dismiss();
                StringUtils.getInstance().showSnackbar(mActivity, R.string.password_remove);
            });
        }
        dialog.show();
        positiveAction.setEnabled(false);
    }

    private void onPasswordStatusChanges(boolean passwordAdded) {
        mEnhancementOptionsEntityArrayList.get(0)
                .setImage(mActivity.getResources()
                        .getDrawable(passwordAdded ?
                                R.drawable.baseline_done_24 : R.drawable.baseline_enhanced_encryption_24));
        mEnhancementOptionsAdapter.notifyDataSetChanged();
    }

    @OnClick(R.id.viewFiles)
    void onViewFilesClick(View view) {
        mBottomSheetUtils.showHideSheet(mSheetBehavior);
    }

    @OnClick(R.id.selectFiles)
    void startAddingPDF(View v) {
        startActivityForResult(mFileUtils.getFileChooser(),
                INTENT_REQUEST_PICK_FILE_CODE);
    }

    @OnClick(R.id.mergebtn)
    void mergeFiles(final View view) {
        String[] pdfpaths = mFilePaths.toArray(new String[0]);
        String masterpwd = mSharedPrefs.getString(MASTER_PWD_STRING, appName);
        new MaterialDialog.Builder(mActivity)
                .title(R.string.creating_pdf)
                .content(R.string.enter_file_name)
                .input(getString(R.string.example), null, (dialog, input) -> {
                    if (StringUtils.getInstance().isEmpty(input)) {
                        StringUtils.getInstance().showSnackbar(mActivity, R.string.snackbar_name_not_blank);
                    } else {
                        if (!mFileUtils.isFileExist(input + getString(R.string.pdf_ext))) {
                            new MergePdf(input.toString(), mHomePath, mPasswordProtected,
                                    mPassword, this, masterpwd).execute(pdfpaths);
                        } else {
                            MaterialDialog.Builder builder = DialogUtils.getInstance().createOverwriteDialog(mActivity);
                            builder.onPositive((dialog12, which) -> new MergePdf(input.toString(),
                                    mHomePath, mPasswordProtected, mPassword,
                                    this, masterpwd).execute(pdfpaths))
                                    .onNegative((dialog1, which) -> mergeFiles(view)).show();
                        }
                    }
                })
                .show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null || resultCode != RESULT_OK || data.getData() == null)
            return;
        if (requestCode == INTENT_REQUEST_PICK_FILE_CODE) {
            //Getting Absolute Path
            String path = RealPathUtil.getInstance().getRealPath(getContext(), data.getData());
            mFilePaths.add(path);
            mMergeSelectedFilesAdapter.notifyDataSetChanged();
            StringUtils.getInstance().showSnackbar(mActivity, getString(R.string.pdf_added_to_list));
            if (mFilePaths.size() > 1 && !mergeBtn.isEnabled())
                setMorphingButtonState(true);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mCheckbtClickTag = savedInstanceState.getString("savText");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getString(R.string.btn_sav_text), mCheckbtClickTag);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
        mFileUtils = new FileUtils(mActivity);
        mBottomSheetUtils = new BottomSheetUtils(mActivity);
    }

    @Override
    public void onItemClick(String path) {
        if (mFilePaths.contains(path)) {
            mFilePaths.remove(path);
            StringUtils.getInstance().showSnackbar(mActivity, getString(R.string.pdf_removed_from_list));
        } else {
            mFilePaths.add(path);
            StringUtils.getInstance().showSnackbar(mActivity, getString(R.string.pdf_added_to_list));
        }

        mMergeSelectedFilesAdapter.notifyDataSetChanged();
        if (mFilePaths.size() > 1) {
            if (!mergeBtn.isEnabled()) setMorphingButtonState(true);
        } else {
            if (mergeBtn.isEnabled()) setMorphingButtonState(false);
        }
    }

    /**
     * resets fragment to initial stage
     */
    @Override
    public void resetValues(boolean isPDFMerged, String path) {
        mMaterialDialog.dismiss();

        if (isPDFMerged) {
            StringUtils.getInstance().getSnackbarwithAction(mActivity, R.string.pdf_merged)
                    .setAction(R.string.snackbar_viewAction,
                            v -> mFileUtils.openFile(path, FileUtils.FileType.e_PDF)).show();
            new DatabaseHelper(mActivity).insertRecord(path,
                    mActivity.getString(R.string.created));
        } else
            StringUtils.getInstance().showSnackbar(mActivity, R.string.file_access_error);

        setMorphingButtonState(false);
        mFilePaths.clear();
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
        mPasswordProtected = false;
        showEnhancementOptions();
    }

    @Override
    public void mergeStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(mActivity);
        mMaterialDialog.show();
    }

    @Override
    public void viewFile(String path) {
        mFileUtils.openFile(path, FileUtils.FileType.e_PDF);
    }

    @Override
    public void removeFile(String path) {
        mFilePaths.remove(path);
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
        StringUtils.getInstance().showSnackbar(mActivity, getString(R.string.pdf_removed_from_list));
        if (mFilePaths.size() < 2 && mergeBtn.isEnabled())
            setMorphingButtonState(false);
    }

    @Override
    public void moveUp(int position) {
        Collections.swap(mFilePaths, position, position - 1);
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
    }

    @Override
    public void moveDown(int position) {
        Collections.swap(mFilePaths, position, position + 1);
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
    }

    private void setMorphingButtonState(Boolean enabled) {
        if (enabled)
            mMorphButtonUtility.morphToSquare(mergeBtn, mMorphButtonUtility.integer());
        else
            mMorphButtonUtility.morphToGrey(mergeBtn, mMorphButtonUtility.integer());

        mergeBtn.setEnabled(enabled);
    }

    @Override
    public void onPopulate(ArrayList<String> paths) {
        CommonCodeUtils.getInstance().populateUtil(mActivity, paths,
                this, mLayout, mLottieProgress, mRecyclerViewFiles);
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