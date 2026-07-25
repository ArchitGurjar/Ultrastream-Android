package com.ultrastream.app.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Search : Screen("search")
    object Addons : Screen("addons")
    object Profile : Screen("profile")
    object Details : Screen("details/{id}/{type}") {
        fun pass(id: String, type: String) =
            "details/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(type, "UTF-8")}"
    }
    object Player : Screen("player/{title}") {
        fun pass(title: String) = "player/${URLEncoder.encode(title, "UTF-8")}"
    }
    // ✅ New: Catalog screen for See All
    object Catalog : Screen("catalog/{rowId}/{title}") {
        fun pass(rowId: String, title: String) =
            "catalog/${URLEncoder.encode(rowId, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
    }
}
