package net.coding.program.project.detail.merge;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import net.coding.program.R;
import net.coding.program.common.BlankViewDisplay;
import net.coding.program.common.Global;
import net.coding.program.common.base.BaseLoadMoreFragment;
import net.coding.program.model.Merge;
import net.coding.program.model.ProjectObject;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

@EFragment(R.layout.fragment_project_merge2)
public class ProjectMergeFragment extends BaseLoadMoreFragment {
    public static final int RESULT_CHANGE = 1;
    private static final String HOST_MERGE = "HOST_MERGE";

    @FragmentArg
    ProjectObject mProjectObject;
    @ViewById
    RadioGroup rgRoot;
    @ViewById
    RadioGroup prRoot;
    @ViewById
    View blankLayout;

    private TextView toolbarTitle;

    private String[] status;
    private String[] statusEng;
    private String[] prStatus;

    private MergeAdapter mMergeAdapter;
    private String mUrlMerge;
    private View.OnClickListener onClickRetry = v -> {
        onRefresh();
        loadMore();
    };

    @AfterViews
    protected final void initProjectMergeFragment() {
        status = getResources().getStringArray(R.array.merge_status);
        statusEng = getResources().getStringArray(R.array.merge_status_english);
        prStatus = getResources().getStringArray(R.array.pr_status);

        toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarProjectTitle);
        toolbarTitle.setText(status[0]);
        Drawable drawable= getResources().getDrawable(R.drawable.arrow_drop_down_green);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        toolbarTitle.setCompoundDrawables(null,null,drawable,null);
        toolbarTitle.setCompoundDrawablePadding(Global.dpToPx(10));
        toolbarTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mProjectObject.is_public) {
                    if(prRoot.getVisibility() == View.GONE||prRoot.getVisibility() == View.INVISIBLE){
                        prRoot.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.INVISIBLE);
                        rgRoot.setVisibility(View.GONE);
                    }else {
                        prRoot.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                }else{
                    if(rgRoot.getVisibility() == View.GONE||rgRoot.getVisibility() == View.INVISIBLE){
                        rgRoot.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.INVISIBLE);
                        prRoot.setVisibility(View.GONE);
                    }else {
                        rgRoot.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        listViewAddHeaderSection(listView);
        listView.setVisibility(View.INVISIBLE);
        initRefreshLayout();

        mMergeAdapter = new MergeAdapter(new ArrayList<>(), this, getImageLoad());
        listView.setAdapter(mMergeAdapter);

        mUrlMerge = mProjectObject.getMergesFilterAll();
        loadMore();
        showDialogLoading();
    }

    @Click
    void toolbarTitle(View v) {
        if (mProjectObject.is_public) {
            if(prRoot.getVisibility() == View.GONE||prRoot.getVisibility() == View.INVISIBLE){
                prRoot.setVisibility(View.VISIBLE);
                listView.setVisibility(View.INVISIBLE);
            }else {
                prRoot.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }
        }else{
            if(rgRoot.getVisibility() == View.GONE||rgRoot.getVisibility() == View.INVISIBLE){
                rgRoot.setVisibility(View.VISIBLE);
                listView.setVisibility(View.INVISIBLE);
            }else {
                rgRoot.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }
        }

    }


    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        hideDialogLoading();
        if (tag.equals(HOST_MERGE)) {
            setRefreshing(false);
            if (code == 0) {
                if (isLoadingFirstPage(HOST_MERGE)) {
                    mMergeAdapter.clearData();
                }

                JSONArray jsonArray = respanse.getJSONObject("data").getJSONArray("list");
                ArrayList<Merge> parseData = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); ++i) {
                    parseData.add(new Merge(jsonArray.getJSONObject(i)));
                }

                mMergeAdapter.appendDataUpdate(parseData);
            } else {
                showErrorMsg(code, respanse);
            }

            updateLoadingState(code, tag, mMergeAdapter.getCount());

            listView.setVisibility(mMergeAdapter.getCount() > 0 ? View.VISIBLE : View.INVISIBLE);
            BlankViewDisplay.setBlank(mMergeAdapter.getCount(), this, code == 0, blankLayout, onClickRetry);
        }
    }

    @Click
    void merge_all() {//全部
        mUrlMerge = mProjectObject.getMergesFilterAll();
        actionStatus(status[0]);
    }

    @Click
    void merge_can_merge() {//可合并
        mUrlMerge = mProjectObject.getMergesFilterStatus(statusEng[1]);
        actionStatus(status[1]);
    }

    @Click
    void merge_can_not_merge() {//不可自动合并
        mUrlMerge = mProjectObject.getMergesFilterStatus(statusEng[2]);
        actionStatus(status[2]);
    }

    @Click
    void merge_refuse() {//已拒绝
        mUrlMerge = mProjectObject.getMergesFilterStatus(statusEng[3]);
        actionStatus(status[3]);
    }

    @Click
    void merge_accept() {//已合并
        mUrlMerge = mProjectObject.getMergesFilterStatus(statusEng[4]);
        actionStatus(status[4]);
    }


    @Click
    void merge_all_pr() {//全部
        mUrlMerge = mProjectObject.getMergesFilterAll();
        actionPrStatus(prStatus[0]);
    }

    @Click
    void merge_open() {//已处理
        mUrlMerge = mProjectObject.getHttpMerge(false);
        actionPrStatus(prStatus[1]);
    }

    @Click
    void merge_close() {//未处理
        mUrlMerge = mProjectObject.getHttpMerge(true);
        actionPrStatus(prStatus[2]);
    }

    @Override
    public void onRefresh() {
        initSetting();
        loadMore();
    }

    @Override
    public void loadMore() {
        getNextPageNetwork(mUrlMerge, HOST_MERGE);
    }

    @ItemClick
    protected final void listView(Merge merge) {
        Fragment fragment = getParentFragment();
        if (fragment == null) fragment = this;
        MergeDetailActivity_.intent(fragment).mMerge(merge).startForResult(RESULT_CHANGE);
    }

    private void actionStatus(String title) {
        setToolbarTitle(title);
        onRefresh();
        setMenuViewGone();
        showDialogLoading();
    }

    private void actionPrStatus(String title) {
        setToolbarTitle(title);
        onRefresh();
        setPrMenuViewGone();
        showDialogLoading();
    }

    private void setMenuViewGone(){
        rgRoot.setVisibility(View.GONE);
    }

    private void setPrMenuViewGone(){
        prRoot.setVisibility(View.GONE);
    }

    private void setToolbarTitle(String title){
        toolbarTitle.setText(title);
    }

}
