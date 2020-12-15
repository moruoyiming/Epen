package com.example.permission.constants;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.example.permission.constants.PermissionConstants.NEED_PERMISSIONS;
import static com.example.permission.constants.PermissionConstants.REQUEST_CODE;


@StringDef({NEED_PERMISSIONS,REQUEST_CODE})
@Retention(RetentionPolicy.SOURCE)
public @interface PermissionConstants {
    String NEED_PERMISSIONS = "need_permission";
    String REQUEST_CODE = "request_code";
}