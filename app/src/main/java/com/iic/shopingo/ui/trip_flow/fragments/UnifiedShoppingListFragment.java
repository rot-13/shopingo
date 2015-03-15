package com.iic.shopingo.ui.trip_flow.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.ListView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.iic.shopingo.R;
import com.iic.shopingo.ui.trip_flow.data.ShoppingList;
import com.iic.shopingo.ui.trip_flow.views.ShoppingListView;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by asafg on 05/03/15.
 */
public class UnifiedShoppingListFragment extends Fragment implements ShoppingListView.OnCallListener {

  private ShoppingListAdapter adapter;
  private List<ShoppingList> shoppingLists;

  @InjectView(R.id.unified_shopping_list_list_view)
  ListView listView;

  @InjectView(R.id.unified_shopping_list_empty_stub)
  ViewStub emptyStateStub;

  public UnifiedShoppingListFragment() {
    shoppingLists = new ArrayList<>();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.unified_shopping_list, container, false);
    ButterKnife.inject(this, view);

    listView.setEmptyView(emptyStateStub);

    adapter = new ShoppingListAdapter(getActivity());
    adapter.setOnCallListener(this);
    for (ShoppingList sl : shoppingLists) {
      adapter.addShoppingList(sl);
    }
    listView.setAdapter(adapter);
    return view;
  }

  public void addShoppingList(ShoppingList shoppingList) {
    shoppingLists.add(shoppingList);
    if (adapter != null) {
      adapter.addShoppingList(shoppingList);
    }
  }

  @Override
  public void onCall(String phoneNumber) {
    Intent intent = new Intent(Intent.ACTION_CALL);
    intent.setData(Uri.parse("tel:" + phoneNumber));
    startActivity(intent);
  }

  static class ShoppingListAdapter extends BaseAdapter implements ShoppingListView.OnCallListener {
    private Context context;
    private List<ShoppingList> shoppingLists;

    private ShoppingListView.OnCallListener listener;

    public ShoppingListAdapter(Context context) {
      this.shoppingLists = new ArrayList<>();
      this.context = context;
    }

    public void setOnCallListener(ShoppingListView.OnCallListener listener) {
      this.listener = listener;
    }

    @Override
    public int getCount() {
      return shoppingLists.size();
    }

    @Override
    public ShoppingList getItem(int position) {
      return shoppingLists.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ShoppingListView itemView;
      if (convertView != null) {
        itemView = (ShoppingListView)convertView;
      } else {
        itemView = new ShoppingListView(context);
        itemView.setOnCallListener(this);
      }
      itemView.setShoppingList(getItem(position));
      return itemView;
    }

    @Override
    public void onCall(String phoneNumber) {
      if (listener != null) {
        listener.onCall(phoneNumber);
      }
    }

    public void addShoppingList(ShoppingList shoppingList) {
      shoppingLists.add(shoppingList);
      notifyDataSetChanged();
    }
  }
}
