package za.ac.nmmu.lift_club;

import android.content.Context;
import android.support.v4.app.ListFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by s210036575 on 2015-07-07.
 */
public class SettingsFragment extends ListFragment {
    public MainActivity ma;
    public ArrayList<String> settings = new ArrayList<String>();

    public SettingsFragment(){}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        this.setListAdapter(new ArrayAdapter<String>(this.getActivity().getApplicationContext(),android.R.layout.simple_list_item_1));
        setEmptyText("");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {

    }
}
