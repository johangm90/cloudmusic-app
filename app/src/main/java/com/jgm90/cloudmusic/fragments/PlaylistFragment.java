package com.jgm90.cloudmusic.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jgm90.cloudmusic.R;
import com.jgm90.cloudmusic.R2;
import com.jgm90.cloudmusic.adapters.PlaylistsAdapter;
import com.jgm90.cloudmusic.data.PlaylistData;
import com.jgm90.cloudmusic.dialogs.PlaylistDialog;
import com.jgm90.cloudmusic.interfaces.DialogCaller;
import com.jgm90.cloudmusic.models.PlaylistModel;
import com.jgm90.cloudmusic.utils.Divider;
import com.jgm90.cloudmusic.utils.SharedUtils;
import com.jgm90.cloudmusic.widgets.VulgryMessageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class PlaylistFragment extends Fragment implements SearchView.OnQueryTextListener, DialogCaller {

    public final static String LIST_STATE_KEY = "recycler_list_state";
    public final static String LIST_ARRAY = "recycler_list_model";

    @BindView(R2.id.message_view)
    public VulgryMessageView message_view;
    @BindView(R2.id.rv_playlists)
    public RecyclerView mRecyclerView;
    public String search_query;
    Parcelable listState;
    private PlaylistsAdapter mAdapter;
    private List<PlaylistModel> mModel;
    private AppCompatActivity activity;
    private RecyclerView.LayoutManager mLayoutManager;
    // unbinder
    private Unbinder unbinder;
    private SearchView searchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);
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
            mAdapter = new PlaylistsAdapter(mModel, activity, this);
            mRecyclerView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
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
        reload();
        if (listState != null) {
            mLayoutManager.onRestoreInstanceState(listState);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            reload();
        }
    }

    @OnClick(R2.id.message_view)
    public void reload() {
        message_view.setVisibility(View.GONE);
        mModel.clear();
        mRecyclerView.setAdapter(null);
        getPlaylists();
    }

    public void getPlaylists() {
        try {
            PlaylistData dao = new PlaylistData(getContext());
            mModel = dao.getAll();
            if (mModel.size() > 0) {
                mAdapter = new PlaylistsAdapter(mModel, activity, this);
                mRecyclerView.setAdapter(mAdapter);
                mAdapter.notifyItemChanged(0);
            } else {
                SharedUtils.showMessage(message_view, R.drawable.ic_library_music_black_24dp, R.string.no_playlists);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_playlists, menu);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            PlaylistDialog dialog = new PlaylistDialog(getContext(), this);
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //mAdapter.filter(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        //mAdapter.filter(query);
        return true;
    }

    @Override
    public void onPositiveCall() {
        reload();
    }
}
