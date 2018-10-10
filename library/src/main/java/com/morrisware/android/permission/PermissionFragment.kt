package com.morrisware.android.permission

import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.support.v4.app.Fragment

/**
 * Created by MorrisWare on 2018/9/7.
 * Email: MorrisWare01@gmail.com
 */
class PermissionFragment : Fragment() {

  companion object {
    const val TAG = "PermissionFragment"
  }

  private var mPermissionsCallback: Callback? = null
  private var mPermissionResult: PermissionResult? = null

  @TargetApi(Build.VERSION_CODES.M)
  fun requestPermissions(array: Array<String>, requestCode: Int, result: PermissionResult) {
    mPermissionResult = result
    requestPermissions(array, requestCode)
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    mPermissionsCallback = object : Callback {
      override fun invoke(vararg args: Any) {
        mPermissionResult?.apply {
          if (onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            mPermissionResult = null
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    mPermissionsCallback?.apply {
      invoke()
      mPermissionsCallback = null
    }
  }

  fun checkPermission(permission: String): Boolean =
    activity?.run {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED
      } else {
        this.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
      }
    } ?: throw IllegalStateException("This fragment must be attached to an activity.")

}
