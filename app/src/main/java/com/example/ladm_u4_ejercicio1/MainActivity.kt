package com.example.ladm_u4_ejercicio1

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    var baseremota = FirebaseFirestore.getInstance()
    val siLecturaContactos = 18
    val siPermiso=1
    val siPermisoReceiver =2
    val siPermisoLectura = 3
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.RECEIVE_SMS),siPermisoReceiver)
        }

        if(ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.READ_SMS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_SMS),siPermisoLectura)
        }
        else{
            leerSMSEntrada()
        }


        button.setOnClickListener {
            if(ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS),siPermiso)
            }
            else{
             /*   if(ListaContactosNodeseados()){

                }*/

                if(editTextTextPersonName.text.equals("NO DESEADO")){
                    mensaje("no se puede manda el mensaje")
                }
                else{
                    envioSMS()
                }

            }
        }
        textView.setOnClickListener {
            try{
                val cursor=baseDatos(this,"ENTRANTES",null,1)
                        .readableDatabase
                        .rawQuery("SELECT * FROM ENTRANTES",null)
                var ultimo =""
                if(cursor.moveToFirst()){
                    do{
                        ultimo="ULTIMO MENSAJE RECIBIDO\n CELULAR ORIGEN: "+
                                cursor.getString(0)+
                                "\nMENSAJE SMS: "+cursor.getString(1)

                    }while (cursor.moveToNext())

                }
                else{
                    ultimo="SIN MENSAJES AUN, TABLA VACIA"
                }
                textView.setText(ultimo)

            }catch (err:SQLiteException){
                Toast.makeText(this,err.message,Toast.LENGTH_LONG)
            }
            insertar()
        }

        if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_CONTACTS), siLecturaContactos
            )
        }

        buttoncontactos.setOnClickListener {
            ListaContactos()
        }

    }

    private fun ListaContactos() {
        var resultado = ""

        val cursorContactos = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,null,null,null
        )
        if (cursorContactos!!.moveToFirst()) {
            do {

                var idContacto = cursorContactos.getString(cursorContactos.getColumnIndex(
                    ContactsContract.Contacts._ID))
                var nombreContacto = cursorContactos.getString(cursorContactos.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME))
                var telefonosContactos  = ""
                if (cursorContactos.getInt(cursorContactos.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0){
                    var cursorCel = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf<String>(idContacto.toString()),null
                    )
                    while (cursorCel!!.moveToNext()){
                        telefonosContactos += cursorCel!!.getString(cursorCel.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER))+
                                "\n"
                    }
                    cursorCel.close()
                }
                resultado += "ID: "+idContacto+"\nNombre: "+nombreContacto+"\nTelefonos:\n"+
                        telefonosContactos+"\n------------\n"
            }while (cursorContactos.moveToNext())
            textView.setText(resultado)
        }else{
            resultado = "CONTACTOS:\nNO HAY CONTACTOS CAPTURADOS"
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == siPermiso){
            envioSMS()
        }
        if(requestCode==siPermisoReceiver){
            mensajeRecibir()
        }
        if(requestCode==siPermisoLectura){
            leerSMSEntrada()
        }
    }

    private fun leerSMSEntrada() {
        var cursor = contentResolver.query(
            Uri.parse("content://sms/"),
                null,null,null,null
        )
        var resultado=""
        if (cursor != null) {
            if(cursor.moveToFirst()){
                var postColumnacelularOrigen =cursor.getColumnIndex("address")
                var postColumnaMensaje = cursor.getColumnIndex("body")
                var postColumnaFecha=cursor.getColumnIndex("date")

                do{
                    val fechamensaje =cursor.getString(postColumnaFecha)
                    resultado+= "ORIGEN: "+cursor.getString(postColumnacelularOrigen)+
                            "\nMENSAJE: " + cursor.getString(postColumnaMensaje)+"\nFECHA: "+
                            Date(fechamensaje.toLong())+
                            "\n--------------------------------\n"

                }while (cursor.moveToNext())

            }
            else{
                resultado="NO HAY SMS EN BANDEJA DE ENTRADA"
            }
            textView3.setText(resultado)
        }
    }

    private fun mensajeRecibir() {
        AlertDialog.Builder(this).setMessage("SE OTORGO RECIBIR").show()
    }


    private fun envioSMS() {
        SmsManager.getDefault().sendTextMessage(editTextPhone.text.toString(),null,editTextTextPersonName.text.toString(),null,null)
        Toast.makeText( this,"SE HA ENVIADO EL SMS", Toast.LENGTH_SHORT).show()
    }

    private fun insertar(){
        var datosInsertar = hashMapOf(
                "CELULAR" to editTextPhone.text.toString(),
                "MENSAJE" to editTextTextPersonName.text.toString()
        )
        baseremota.collection("ENTRANTES")
                .add(datosInsertar)
                .addOnSuccessListener {
                    alerta("SE INCERTO EN FIREBASE")
                }
                .addOnFailureListener {
                    mensaje("ERROR${it.message}")
                }
    }

    private fun alerta(s: String) {
        Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    }

    private fun mensaje(s: String) {
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("ATENCION")
                .setMessage(s)
                .setPositiveButton("OK"){d,i->}
                .show()
    }

    private fun ListaContactosNodeseados(){
        var resultado = ""

        val cursorContactos = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,null,null,null
        )
        if (cursorContactos!!.moveToFirst()) {
            do {

                var idContacto = cursorContactos.getString(cursorContactos.getColumnIndex(
                        ContactsContract.Contacts._ID))
                var nombreContacto = cursorContactos.getString(cursorContactos.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME))
                var telefonosContactos  = ""
                if (cursorContactos.getInt(cursorContactos.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0){
                    var cursorCel = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf<String>(idContacto.toString()),null
                    )
                    while (cursorCel!!.moveToNext()){
                        telefonosContactos += cursorCel!!.getString(cursorCel.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER))+
                                "\n"
                    }
                    cursorCel.close()
                }

                if(nombreContacto.equals("NO DESEADO")){
                    resultado += "ID: "+idContacto+"\nNombre: "+nombreContacto+"\nTelefonos:\n"+
                            telefonosContactos+"\n------------\n"
                }
                else{
                    resultado+=""
                }


            }while (cursorContactos.moveToNext())
            textView.setText(resultado)
        }else{
            resultado = "CONTACTOS:\nNO HAY CONTACTOS CAPTURADOS"
        }
    }



}