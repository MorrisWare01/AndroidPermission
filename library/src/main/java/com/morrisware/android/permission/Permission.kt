package com.morrisware.android.permission

import android.arch.lifecycle.LiveData
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.util.ArrayMap
import android.util.SparseArray
import java.util.*

/**
 * Created by MorrisWare on 2018/9/7.
 * Email: MorrisWare01@gmail.com
 */
class Permission : PermissionResult {

  private lateinit var fragmentManager: FragmentManager

  constructor(activity: FragmentActivity) {
    fragmentManager = activity.supportFragmentManager
  }

  constructor(fragment: Fragment) {
    fragmentManager = fragment.childFragmentManager
  }

  private val permissionFragment by lazy {
    val fragment = fragmentManager.findFragmentByTag(PermissionFragment.TAG) ?: PermissionFragment()
    if (!fragment.isAdded) {
      fragmentManager.beginTransaction()
        .add(fragment, PermissionFragment.TAG)
        .commitNow()
    }
    fragment as PermissionFragment
  }

  private val mCallbacks by lazy {
    SparseArray<Callback>()
  }

  private var mRequestCode = 0

  fun checkPermission(permission: String): Boolean = permissionFragment.checkPermission(permission)

  fun shouldShowRequestPermissionRationale(permission: String): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      false
    } else {
      permissionFragment.shouldShowRequestPermissionRationale(permission)
    }

  fun requestPermission(permission: String): LiveData<Result> =
    object : LiveData<Result>() {
      override fun onActive() {
        super.onActive()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          value = if (permissionFragment.checkPermission(permission))
            Result.GRANTED
          else
            Result.DENIED
        } else if (permissionFragment.checkPermission(permission)) {
          value = Result.GRANTED
        } else {
          mCallbacks.put(mRequestCode, object : Callback {
            override fun invoke(vararg args: Any) {
              val results = args[0] as IntArray
              value = if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
                Result.GRANTED
              } else {
                if (permissionFragment.shouldShowRequestPermissionRationale(permission)) {
                  Result.DENIED
                } else {
                  Result.NEVER_ASK_AGAIN
                }
              }
            }
          })

          permissionFragment.requestPermissions(arrayOf(permission), mRequestCode, this@Permission)
          mRequestCode++
        }
      }
    }

  fun requestMultiplePermissions(permissions: Array<String>): LiveData<Map<String, Result>> =
    object : LiveData<Map<String, Result>>() {
      override fun onActive() {
        super.onActive()
        val grantedPermissions = ArrayMap<String, Result>()

        val permissionsToCheck = ArrayList<String>()

        for (permission in permissions) {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            grantedPermissions[permission] = if (permissionFragment.checkPermission(permission))
              Result.GRANTED
            else
              Result.DENIED
          } else if (permissionFragment.checkPermission(permission)) {
            grantedPermissions[permission] = Result.GRANTED
          } else {
            permissionsToCheck.add(permission)
          }
        }

        if (permissionsToCheck.isEmpty()) {
          value = grantedPermissions
        } else {
          mCallbacks.put(mRequestCode, object : Callback {
            override fun invoke(vararg args: Any) {
              val results = args[0] as IntArray
              for ((index, permission) in permissionsToCheck.withIndex()) {
                if (results.isNotEmpty() && results[index] == PackageManager.PERMISSION_GRANTED) {
                  grantedPermissions[permission] = Result.GRANTED
                } else {
                  if (permissionFragment.shouldShowRequestPermissionRationale(permission)) {
                    grantedPermissions[permission] = Result.DENIED
                  } else {
                    grantedPermissions[permission] = Result.NEVER_ASK_AGAIN
                  }
                }
              }
              value = grantedPermissions
            }
          })

          permissionFragment.requestPermissions(permissionsToCheck.toTypedArray(), mRequestCode, this@Permission)
          mRequestCode++
        }
      }
    }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
    mCallbacks.get(requestCode).invoke(grantResults)
    mCallbacks.remove(requestCode)
    return mCallbacks.size() == 0
  }

}
