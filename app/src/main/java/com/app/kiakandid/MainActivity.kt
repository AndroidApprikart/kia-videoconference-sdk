package com.app.kiakandid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.kiakandid.databinding.ActivityMainBinding
import com.app.vc.VCDynamicActivity4
import com.app.vc.virtualroomlist.VirtualRoomListActivity

class MainActivity : AppCompatActivity() {
    lateinit var button: Button
    lateinit var  btnServicePerson:android.widget.Button
    lateinit var  btnCustomer:android.widget.Button
    lateinit var  meeting_code: EditText
    lateinit var  keccode:android.widget.EditText
    lateinit var  kec_name:android.widget.EditText
    lateinit var  paas_code:android.widget.EditText
    lateinit var  roNo:android.widget.EditText
    lateinit var  customerCode:android.widget.EditText
    lateinit var  dealerCode:android.widget.EditText
    lateinit var  displayName:android.widget.EditText
    lateinit var  userName:android.widget.EditText
    lateinit var  userId:android.widget.EditText
    lateinit var  password:android.widget.EditText
    lateinit var  deviceToken:android.widget.EditText
    lateinit var  vcEndTime:android.widget.EditText
    lateinit var  btnOpenVirtualChat:android.widget.Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.button1) as Button
        meeting_code = findViewById(R.id.meeting_code) as EditText
        keccode = findViewById<EditText>(R.id.kec_code)as EditText
        kec_name = findViewById<EditText>(R.id.kec_name)as EditText
        paas_code = findViewById<EditText>(R.id.password)as EditText
        roNo = findViewById<EditText>(R.id.rono)as EditText
        customerCode = findViewById<EditText>(R.id.customerCode)as EditText
        dealerCode = findViewById<EditText>(R.id.dealerCode)as EditText
        displayName = findViewById<EditText>(R.id.displayname)as EditText
        userName = findViewById<EditText>(R.id.et_username_or_phone)as EditText
        btnServicePerson = findViewById<Button>(R.id.btn_service_person)as Button
        btnCustomer = findViewById<Button>(R.id.btn_customer)as Button
        userId = findViewById<EditText>(R.id.et_user_id)as EditText
        password = findViewById<EditText>(R.id.et_password)as EditText
        deviceToken = findViewById<EditText>(R.id.et_device_Token_login)as EditText
        vcEndTime = findViewById<EditText>(R.id.et_vc_end_time)as EditText
        btnOpenVirtualChat = findViewById<Button>(R.id.btn_open_virtual_chat) as Button

        button.setOnClickListener(View.OnClickListener { //                if(kec_name.equals("SERVICE_PERSON")){
            //                    Intent intent=new Intent(MainActivity.this\n vCScreenServiceAdviosor.class);
            //                intent.putExtra("room",meeting_code.getText().toString());
            //                intent.putExtra("service_person_id",keccode.getText().toString());
            //                intent.putExtra("user_type","SERVICE_PERSON");
            //                intent.putExtra("auth_passcode",paas_code.getText().toString());
            //                    startActivity(intent);
            //                }else{
            if (!kec_name.getText().toString().isEmpty()) {
                val intent = Intent(
                    this@MainActivity,
                    VCDynamicActivity4::class.java
                )
                intent.putExtra("room", meeting_code.getText().toString())
                intent.putExtra("service_person_id", keccode.getText().toString())
                intent.putExtra("user_type", kec_name.getText().toString())
                intent.putExtra("auth_passcode", password.getText().toString())
                intent.putExtra("roNo", meeting_code.getText().toString())
                intent.putExtra("customerCode", keccode.getText().toString())
                intent.putExtra("dealerCode", kec_name.getText().toString())
                intent.putExtra("displayName", displayName.getText().toString())
                intent.putExtra("userName", userName.getText().toString())
                intent.putExtra("userId", userId.getText().toString())
                intent.putExtra("password", password.getText().toString())
                intent.putExtra("deviceToken", deviceToken.getText().toString())
                intent.putExtra("vcEndTime", vcEndTime.getText().toString())
                startActivity(intent)
            } else {
                Toast.makeText(applicationContext, "Fields cannot be empty", Toast.LENGTH_SHORT)
                    .show()
            }
        })
        btnServicePerson.setOnClickListener(View.OnClickListener {
            val intent = Intent(
                this@MainActivity,
                VCDynamicActivity4::class.java
            )
            intent.putExtra("testUserType", "SERVICE_PERSON")
            intent.putExtra("userId", userId.getText().toString())
            intent.putExtra("password", password.getText().toString())
            intent.putExtra("deviceToken", deviceToken.getText().toString())
            intent.putExtra("vcEndTime",vcEndTime.getText().toString())
            startActivity(intent)
        })
        btnCustomer!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(
                this@MainActivity,
                VCDynamicActivity4::class.java
            )
            intent.putExtra("testUserType", "customer")
            intent.putExtra("userId", userId.getText().toString())
            intent.putExtra("password", password.getText().toString())
            intent.putExtra("deviceToken", deviceToken.getText().toString())
            intent.putExtra("vcEndTime",vcEndTime.getText().toString())
            startActivity(intent)
        })

        btnOpenVirtualChat.setOnClickListener {
            val intent = Intent(this@MainActivity, VirtualRoomListActivity::class.java)
            startActivity(intent)
        }
    }
}