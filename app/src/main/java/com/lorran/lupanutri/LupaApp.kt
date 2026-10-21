package com.lorran.lupanutri

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.lorran.lupanutri.data.OffApi
import com.lorran.lupanutri.data.ProductRepository
import com.lorran.lupanutri.data.ProfileStore
import com.lorran.lupanutri.data.ShoppingRepository
import com.lorran.lupanutri.data.db.AppDatabase
import com.lorran.lupanutri.domain.Taco

class LupaApp : Application() {
    private val db by lazy { AppDatabase.build(this) }
    private val taco: Lazy<Taco?> = lazy {
        runCatching { assets.open("taco.tsv").bufferedReader().useLines { Taco.parse(it) } }.getOrNull()
    }
    val api by lazy { OffApi() }
    val repository by lazy { ProductRepository(db.products(), api, taco) }
    /** Para tarefas que devem terminar mesmo depois de sair da tela (ex.: envio ao Open Food Facts). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val shopping by lazy { ShoppingRepository(db.shopping(), repository) }
    val profileStore by lazy { ProfileStore(this) }
}
