package com.morrisware.android.permission

/**
 * Created by MorrisWare on 2018/9/7.
 * Email: MorrisWare01@gmail.com
 */
interface PermissionResult {

  fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean

}
