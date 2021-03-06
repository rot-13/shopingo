package com.iic.shopingo.api.request;

import com.iic.shopingo.api.ApiResult;
import com.iic.shopingo.api.BaseApiCommand;
import com.iic.shopingo.api.Constants;
import com.iic.shopingo.api.Server;
import com.iic.shopingo.api.models.ApiSimpleResponse;

/**
 * Created by asafg on 11/03/15.
 */
public class CancelRequestCommand extends BaseApiCommand<ApiResult> {

  public CancelRequestCommand(String authToken) {
    super(authToken);
  }

  @Override
  public ApiResult executeSync() {
    try {
      ApiSimpleResponse response = Server.post(authToken, Constants.Routes.REQUESTS_CANCEL_PATH, ApiSimpleResponse.class);
      return new ApiResult(response);
    } catch (Exception e) {
      return new ApiResult(e.getMessage());
    }
  }
}
