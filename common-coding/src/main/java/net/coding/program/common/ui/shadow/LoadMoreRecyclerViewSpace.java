package net.coding.program.common.ui.shadow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.coding.program.common.GlobalCommon;

/**
 * Created by chenchao on 16/3/4.
 * 普通的分割线，左边默认空 15dp
 */
public class LoadMoreRecyclerViewSpace extends BaseRecyclerViewSpace {

    private int leftSapce;

    public LoadMoreRecyclerViewSpace(Context context) {
        super(context);
        leftSapce = GlobalCommon.dpToPx(15);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos == 0) {
            outRect.top = topSpace;
            outRect.bottom = lineSpace;
        }

        if (pos == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = 0;
        } else if (pos == parent.getAdapter().getItemCount() - 2) {
            outRect.bottom = bottomSpace;
        } else {
            outRect.bottom = lineSpace;
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View view = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(view);
            if (pos == 0) {
                Rect rectTop = new Rect(view.getLeft(), view.getTop() - topSpace, view.getRight(), view.getTop());
                c.drawRect(rectTop, paintDivide);

                Rect rectShadow = new Rect(rectTop);
                rectShadow.top = rectShadow.bottom - shadowHigh;
                shadowTop.setBounds(rectShadow);
                shadowTop.draw(c);
            }

            if (pos == parent.getAdapter().getItemCount() - 1) {

            } else if (pos == parent.getAdapter().getItemCount() - 2) {
                Rect rect = new Rect(view.getLeft(), view.getBottom(), view.getRight(), view.getBottom() + bottomSpace);
                c.drawRect(rect, paintDivide);

                Rect rectShadow = new Rect(rect);
                rectShadow.bottom = view.getTop() + shadowHigh;
                shadowBottom.setBounds(rectShadow);
                shadowBottom.draw(c);
            } else {
                Rect rectDivideLine = new Rect(view.getLeft(), view.getBottom(), view.getLeft() + leftSapce, view.getBottom() + lineSpace);
                c.drawRect(rectDivideLine, paintBg);
                rectDivideLine.left = rectDivideLine.right;
                rectDivideLine.right = view.getRight();
                c.drawRect(rectDivideLine, paintDivideLine);
            }

        }

    }
}
