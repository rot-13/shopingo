package com.iic.shopingo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;
import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.iic.shopingo.R;
import com.iic.shopingo.api.user.LoginCommand;
import com.iic.shopingo.api.user.UserApiResult;
import com.iic.shopingo.dal.models.UserInfo;
import com.iic.shopingo.services.CurrentUser;
import com.iic.shopingo.services.FacebookConnector;
import com.iic.shopingo.services.NotificationsHelper;
import com.iic.shopingo.utils.FacebookUtils;

public class OnboardingActivity extends ActionBarActivity {

  private static final String LOG_TAG = OnboardingActivity.class.getSimpleName();

  @InjectView(R.id.onboarding_login_btn)
  LoginButton loginBtn;

  private UiLifecycleHelper facebookLifecycleHelper;

  private boolean duringLogin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_onboarding);

    ButterKnife.inject(this);
    Log.d("KeyHash", FacebookUtils.getKeyHash(this));

    loginBtn.setReadPermissions("public_profile", "user_location");
    facebookLifecycleHelper = new UiLifecycleHelper(this, new Session.StatusCallback() {
      @Override
      public void call(Session session, SessionState sessionState, Exception e) {
        onSessionStateChanged(session, sessionState, e);
      }
    });
    facebookLifecycleHelper.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    facebookLifecycleHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    facebookLifecycleHelper.onPause();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    facebookLifecycleHelper.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    facebookLifecycleHelper.onSaveInstanceState(outState);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    facebookLifecycleHelper.onActivityResult(requestCode, resultCode, data);
  }

  private void onSessionStateChanged(Session session, SessionState sessionState, Exception e) {
    if (sessionState.isOpened() && !duringLogin) {
      Log.d(LOG_TAG, "Logged in to facebook");
      login(session);
    } else if (sessionState.isClosed()) {
      Log.d(LOG_TAG, "Facebook session is closed");
      logout();
    }
  }

  private void login(Session session) {
    duringLogin = true;
    FacebookConnector.login(session).continueWith(new Continuation<UserInfo, Void>() {
      @Override
      public Void then(Task<UserInfo> task) throws Exception {
        if (task.isFaulted()) {
          Toast.makeText(OnboardingActivity.this, "Could not login: " + task.getError(), Toast.LENGTH_LONG).show();
          return null;
        }

        if (task.isCancelled()) {
          // task can also be cancelled, in that case we do nothing.
          return null;
        }

        final CurrentUser currentUser = CurrentUser.getInstance();
        UserInfo info = task.getResult();
        ApiTask<UserApiResult> apiTask = new ApiTask<UserApiResult>(getSupportFragmentManager(), "Logging you in...",
            new LoginCommand(info.getUid(), info.getFirstName(), info.getLastName(), info.getStreet(), info.getCity(),
                info.getPhoneNumber()));

        apiTask.execute().continueWith(new Continuation<UserApiResult, Object>() {
          @Override
          public Object then(Task<UserApiResult> task) throws Exception {
            if (task.isFaulted() || task.isCancelled()) {
              Toast.makeText(OnboardingActivity.this, "Could not login: " + task.getError(), Toast.LENGTH_LONG).show();
              return null;
            }

            if (task.isCancelled()) {
              Toast.makeText(OnboardingActivity.this, "Login aborted", Toast.LENGTH_LONG).show();
              return null;
            }

            currentUser.state = task.getResult().userState;
            currentUser.userInfo = task.getResult().userContactInfo;
            currentUser.save();
            Intent intent = new Intent(OnboardingActivity.this, ContactDetailsActivity.class);
            startActivity(intent);
            finish();
            duringLogin = false;
            return null;
          }
        }, Task.UI_THREAD_EXECUTOR).continueWith(new Continuation<Object, Void>() {
          @Override
          public Void then(Task<Object> task) throws Exception {
            if (CurrentUser.getToken() != null) {
              NotificationsHelper.registerForNotificationsAsync(OnboardingActivity.this);
            }
            return null;
          }
        });

        return null;
      }
    }, Task.UI_THREAD_EXECUTOR);
  }

  private void logout() {
    CurrentUser.getInstance().logout();
  }
}
