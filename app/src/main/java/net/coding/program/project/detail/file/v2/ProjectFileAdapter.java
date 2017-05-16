package net.coding.program.project.detail.file.v2;

import android.view.View;

import com.marshalchen.ultimaterecyclerview.quickAdapter.easyRegularAdapter;

import net.coding.program.R;
import net.coding.program.network.model.file.CodingFile;

import java.util.List;
import java.util.Set;

/**
 * Created by chenchao on 2017/5/15.
 */
public class ProjectFileAdapter extends easyRegularAdapter<CodingFile, ProjectFileHolder> {

    private boolean editMode = false;
    private Set<CodingFile> selectFiles;

    public void setEditMode(boolean editMode) {
        if (this.editMode != editMode) {
            this.editMode = editMode;
            notifyDataSetChanged();
        }
    }

    public ProjectFileAdapter(List<CodingFile> list, Set<CodingFile> selectFiles) {
        super(list);
        this.selectFiles = selectFiles;
    }

    @Override
    protected int getNormalLayoutResId() {
        return R.layout.project_attachment_file_list_item ;
    }

    @Override
    protected ProjectFileHolder newViewHolder(View view) {
        return new ProjectFileHolder(view);
    }

    @Override
    protected void withBindHolder(ProjectFileHolder holder, CodingFile data, int position) {
        holder.bind(data, editMode, selectFiles);
    }

//    @Override
//    public ProjectFileHolder newFooterHolder(View view) {
//        return new ProjectFileHolder(view);
//    }
//
//    @Override
//    public ProjectFileHolder newHeaderHolder(View view) {
//        return new ProjectFileHolder(view);
//    }
//
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        return super.onCreateViewHolder(parent, viewType);
//    }
//
//    @Override
//    public UltimateRecyclerviewViewHolder onCreateViewHolder(ViewGroup parent) {
//        return super.onCreateViewHolder(parent);
//    }
}
