package com.jgm90.cloudmusic.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.R2;
import com.jgm90.cloudmusic.adapters.SearchAdapter;
import com.jgm90.cloudmusic.interfaces.RestInterface;
import com.jgm90.cloudmusic.models.SongModel;
import com.jgm90.cloudmusic.utils.Divider;
import com.jgm90.cloudmusic.utils.NetworkHelper;
import com.jgm90.cloudmusic.utils.RestClient;
import com.jgm90.cloudmusic.utils.SharedUtils;
import com.jgm90.cloudmusic.widgets.VulgryMessageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends BaseFragment implements SearchView.OnQueryTextListener {

    public final static String LIST_STATE_KEY = "recycler_list_state";
    public final static String LIST_ARRAY = "recycler_list_model";

    @BindView(R2.id.message_view)
    public VulgryMessageView message_view;
    @BindView(R2.id.loader)
    public MaterialProgressBar loader;
    @BindView(R2.id.rv_search)
    public RecyclerView mRecyclerView;
    public String search_query;
    Parcelable listState;
    private SearchAdapter mAdapter;
    private List<SongModel> mModel;
    private AppCompatActivity activity;
    private RecyclerView.LayoutManager mLayoutManager;
    // unbinder
    private Unbinder unbinder;
    private SearchView searchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);
        activity = (AppCompatActivity) getActivity();
        unbinder = ButterKnife.bind(this, rootView);
        mLayoutManager = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new Divider(activity));
        mRecyclerView.getItemAnimator().setAddDuration(SharedUtils.rv_anim_duration);
        mRecyclerView.setAdapter(null);
        mModel = new ArrayList<>();
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            listState = savedInstanceState.getParcelable(LIST_STATE_KEY);
            mModel = savedInstanceState.getParcelableArrayList(LIST_ARRAY);
            mAdapter = new SearchAdapter(mModel, activity);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
        if (mModel.size() == 0) {
            SharedUtils.showMessage(message_view, R.drawable.ic_search_black_24dp, R.string.search_message);
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        listState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable(LIST_STATE_KEY, listState);
        outState.putParcelableArrayList(LIST_ARRAY, new ArrayList<>(mModel));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listState != null) {
            mLayoutManager.onRestoreInstanceState(listState);
        }
    }

    @OnClick(R2.id.message_view)
    public void reload() {
        if (search_query.length() > 0) {
            if (NetworkHelper.isOnline(getContext())) {
                message_view.setVisibility(View.GONE);
                mModel.clear();
                getResult(search_query);
                searchView.clearFocus();
            } else {
                SharedUtils.showMessage(message_view, R.drawable.ic_cloud_off_black_24dp, R.string.error_offline);
            }
        }
    }

    public void getResult(String query) {
        loader.setVisibility(View.VISIBLE);
        try {
            RestInterface api = RestClient.build(SharedUtils.server);
            Call<List<SongModel>> call = api.getSongs(query, 1, 50);
            call.enqueue(new Callback<List<SongModel>>() {
                @Override
                public void onResponse(Call<List<SongModel>> call, Response<List<SongModel>> response) {
                    loader.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        if (response.body() != null && response.body().size() > 0) {
                            for (int i = 0; i < response.body().size(); i++) {
                                mModel = response.body();
                            }
                            mAdapter = new SearchAdapter(mModel, activity);
                            mRecyclerView.setAdapter(mAdapter);
                            mAdapter.notifyItemChanged(0);
                        }
                    } else {
                        SharedUtils.showMessage(message_view, R.drawable.ic_error_black_24dp, R.string.error_retrofit);
                    }
                }

                @Override
                public void onFailure(Call<List<SongModel>> call, Throwable t) {
                    loader.setVisibility(View.GONE);
                    SharedUtils.showMessage(message_view, R.drawable.ic_cloud_off_black_24dp, R.string.error_server);
                }
            });
        } catch (Exception e) {
            loader.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint("Buscar");
        searchView.setOnQueryTextListener(this);
        if (!TextUtils.isEmpty(search_query)) {
            searchView.setQuery(search_query, false);
            searchView.setIconified(false);
            searchView.clearFocus();
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        search_query = newText;
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mModel.clear();
        mRecyclerView.setAdapter(null);
        reload();
        return true;
    }
}

