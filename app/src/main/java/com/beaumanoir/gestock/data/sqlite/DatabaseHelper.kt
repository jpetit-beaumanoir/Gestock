package com.beaumanoir.gestock.data.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("PRAGMA foreign_keys = ON;")
        db?.execSQL("CREATE TABLE IF NOT EXISTS almacenes (codigo INTEGER PRIMARY KEY,nombre TEXT NOT NULL)")
        db?.execSQL("CREATE TABLE IF NOT EXISTS palets (palet INTEGER NOT NULL, almacen INTEGER NOT NULL, PRIMARY KEY (palet, almacen),FOREIGN KEY (almacen) REFERENCES almacenes(codigo))")
        db?.execSQL("CREATE TABLE IF NOT EXISTS cajas (caja INTEGER NOT NULL,palet INTEGER NOT NULL,almacen INTEGER NOT NULL,temporada TEXT,descripcion TEXT,cantidad INTEGER NOT NULL,PRIMARY KEY (caja,palet,almacen),FOREIGN KEY (palet,almacen) REFERENCES palets(palet,almacen))")
        db?.execSQL("CREATE TABLE IF NOT EXISTS productos (ean TEXT PRIMARY KEY, nombre TEXT NOT NULL, familia TEXT NOT NULL,subfamilia TEXT NOT NULL, color TEXT NOT NULL, talla TEXT NOT NULL, pvp REAL NOT NULL,prmp REAL NOT NULL, temporada TEXT NOT NULL, marca TEXT NOT NULL)")
        db?.execSQL("CREATE TABLE IF NOT EXISTS stock (id INTEGER PRIMARY KEY AUTOINCREMENT,ean TEXT NOT NULL,palet INTEGER NOT NULL,caja INTEGER NOT NULL,almacen INTEGER NOT NULL,FOREIGN KEY (ean) REFERENCES productos(ean),FOREIGN KEY (palet, caja, almacen) REFERENCES cajas(palet, caja, almacen))")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("PRAGMA foreign_keys = OFF;")
        db?.execSQL("DROP TABLE IF EXISTS palets")
        db?.execSQL("DROP TABLE IF EXISTS almacenes")
        db?.execSQL("DROP TABLE IF EXISTS cajas")
        db?.execSQL("DROP TABLE IF EXISTS productos")
        db?.execSQL("DROP TABLE IF EXISTS stock")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "gestock.db"
        private const val DATABASE_VERSION = 2
    }
}
