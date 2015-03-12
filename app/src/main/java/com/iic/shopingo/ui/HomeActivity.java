package com.iic.shopingo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.iic.shopingo.R;
import com.iic.shopingo.api.ApiResult;
import com.iic.shopingo.api.trip.StartTrip;
import com.iic.shopingo.api.user.CurrentStateApiResult;
import com.iic.shopingo.api.user.GetCurrentState;
import com.iic.shopingo.dal.models.IncomingRequest;
import com.iic.shopingo.services.CurrentUser;
import com.iic.shopingo.services.FacebookConnector;
import com.iic.shopingo.ui.request_flow.activities.RequestStateActivity;
import com.iic.shopingo.ui.request_flow.activities.SelectShopperActivity;
import com.iic.shopingo.ui.trip_flow.activities.ManageTripActivity;
import java.util.ArrayList;
import java.util.Arrays;

public class HomeActivity extends ActionBarActivity {

  @InjectView(R.id.home_content_container)
  View contentContainer;

  @InjectView(R.id.home_spinner_container)
  View spinnerContainer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);
    ButterKnife.inject(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (CurrentUser.getInstance().state == CurrentUser.State.LOGGED_OUT) {
      navigateToOnboarding();
    } else {
      showSpinner();

      new GetCurrentState(CurrentUser.getToken()).executeAsync().continueWith(new Continuation<CurrentStateApiResult, Void>() {
        @Override
        public Void then(Task<CurrentStateApiResult> task) throws Exception {
          if (!task.isFaulted() && !task.isCancelled()) {
            switch (task.getResult().userState) {
              case LOGGED_OUT: {
                Log.e("HomeActivity", "Server should not report user as logged out");
                finish();
              } break;
              case IDLE: {
                // TODO: a user might still have a declined request that he never saw. redirect to the declined request here.
                showContent();
              } break;
              case TRIPPING: {
                CurrentUser.getInstance().state = CurrentUser.State.TRIPPING;
                Intent intent = new Intent(HomeActivity.this, ManageTripActivity.class);
                ArrayList<IncomingRequest> list = new ArrayList<>(task.getResult().activeTripRequests.size());
                list.addAll(task.getResult().activeTripRequests);
                intent.putParcelableArrayListExtra(ManageTripActivity.EXTRA_REQUESTS, list);
                startActivity(intent);
              } break;
              case REQUESTING: {
                CurrentUser.getInstance().state = CurrentUser.State.REQUESTING;
                Intent intent = new Intent(HomeActivity.this, RequestStateActivity.class);
                intent.putExtra(RequestStateActivity.EXTRAS_REQUEST_KEY, task.getResult().activeOutgoingRequest);
                startActivity(intent);
              } break;
            }
          } else {
            Log.e("HomeActivity", "Can't contact server", task.getError());
            finish();
          }
          return null;
        }
      }, Task.UI_THREAD_EXECUTOR);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_home, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    switch (id) {
      case R.id.action_logout:
        FacebookConnector.logout(this);
        CurrentUser.getInstance().logout();
        navigateToOnboarding();
        break;
      case R.id.action_update_contact:
        Intent intent = new Intent(this, ContactDetailsActivity.class);
        startActivity(intent);
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @OnClick(R.id.home_create_request_btn)
  public void onCreateRequest(View view) {
    Intent intent = new Intent(this, SelectShopperActivity.class);
    startActivity(intent);
  }

  @OnClick(R.id.home_go_shopping_btn)
  public void onGoShopping(View view) {

    ApiTask<ApiResult> task = new ApiTask<>(getSupportFragmentManager(), "Starting trip...", new StartTrip(CurrentUser.getToken()));

    task.execute().continueWith(new Continuation<ApiResult, Void>() {
      @Override
      public Void then(Task<ApiResult> task) throws Exception {
        if (!task.isFaulted() && !task.isCancelled()) {
          CurrentUser.getInstance().state = CurrentUser.State.TRIPPING;
          Intent intent = new Intent(HomeActivity.this, ManageTripActivity.class);
          startActivity(intent);
        } else {
          Toast.makeText(HomeActivity.this, "Could not start trip: " + task.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
        return null;
      }
    }, Task.UI_THREAD_EXECUTOR);
  }

  private void navigateToOnboarding() {
    Intent intent = new Intent(this, OnboardingActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
    finish();
  }

  private void showSpinner() {
    contentContainer.setVisibility(View.GONE);
    spinnerContainer.setVisibility(View.VISIBLE);
  }

  private void showContent() {
    spinnerContainer.setVisibility(View.GONE);
    contentContainer.setVisibility(View.VISIBLE);
  }
}