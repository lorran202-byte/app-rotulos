package com.lorran.lupanutri

import android.app.Application
import com.lorran.lupanutri.data.OffApi
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.db.AppDatabase

class LupaApp : Application() {
    val repository by lazy { ProductRepository(AppDatabase.build(this).products(), OffApi()) }
    val profileStore by lazy { ProfileStore(this) }
}
