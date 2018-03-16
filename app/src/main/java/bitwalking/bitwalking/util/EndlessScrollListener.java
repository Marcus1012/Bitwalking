package bitwalking.bitwalking.util;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by Marcus on 12/25/16.
 */

public abstract class EndlessScrollListener extends RecyclerView.OnScrollListener {
    private int totalItemCount;
    private int lastVisibleItem;
    private int visibleThreshold = 1;
    private int previousTotalItemCount = 0;
    private boolean isLoading = true;

    LinearLayoutManager linearLayoutManager;

    public EndlessScrollListener(LinearLayoutManager linearLayoutManager) {
        this.linearLayoutManager = linearLayoutManager;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        totalItemCount = linearLayoutManager.getItemCount();
        lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

        if (totalItemCount < previousTotalItemCount) {
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) { this.isLoading = true; }
        }

        if (isLoading && (totalItemCount > previousTotalItemCount)) {
            if (totalItemCount > previousTotalItemCount + 1)
                isLoading = false;
            previousTotalItemCount = totalItemCount;
        }

        if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
            isLoading = onLoadMore(lastVisibleItem);
        }
    }

    // Defines the process for actually loading more data based on page
    // Returns true if more data is being loaded; returns false if there is no more data to load.
    public abstract boolean onLoadMore(int lastVisibleItem);
}