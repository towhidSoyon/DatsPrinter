package com.towhid.myapplication

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("bluetooth") {
            BluetoothScreen(navController = navController)
        }

        /*composable("pos_print") {
            PosPrintScreen(navController = navController)
        }*/
    }
}


