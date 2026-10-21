package com.lorran.lupanutri

import android.app.Application
import com.lorran.lupanutri.data.OffApi
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.ShoppingRepository
import com.lorran.lupanutri.data.db.AppDatabase

class LupaApp : Application() {
    private val db by lazy { AppDatabase.build(this) }
    val repository by lazy { ProductRepository(db.products(), OffApi()) }
    val shopping by lazy { ShoppingRepository(db.shopping(), repository) }
    val profileStore by lazy { ProfileStore(this) }
}
